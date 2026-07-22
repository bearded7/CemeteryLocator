package com.example.cemeterylocator.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cemeterylocator.R
import com.example.cemeterylocator.api.ApiClient
import com.example.cemeterylocator.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: GraveRecordAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = GraveRecordAdapter { record ->
            val intent = Intent(this, PersonDetailActivity::class.java)
            intent.putExtra(PersonDetailActivity.EXTRA_GRAVENO, record.graveNo)
            intent.putExtra(PersonDetailActivity.EXTRA_LOCATION, record.location)
            startActivity(intent)
        }
        binding.recyclerResults.layoutManager = LinearLayoutManager(this)
        binding.recyclerResults.adapter = adapter

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                debounceSearch(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.swipeRefresh.setOnRefreshListener {
            runSearch(binding.editSearch.text.toString())
        }

        // Empty query on load shows everything (search.php with blank q
        // matches all rows via the LIKE '%%' pattern).
        runSearch("")
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionsMenu() // reflect login/logout state changes made elsewhere
    }

    private fun debounceSearch(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(300) // avoid firing a request on every keystroke
            runSearch(query)
        }
    }

    private fun runSearch(query: String) {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val api = ApiClient.create(this@MainActivity)
                val response = api.search(query)
                if (response.isSuccessful) {
                    val results = response.body()?.results.orEmpty()
                    adapter.submitList(results)
                    binding.textEmpty.visibility =
                        if (results.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Server error: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Couldn't reach server: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val loggedIn = com.example.cemeterylocator.api.SessionManager.isLoggedIn(this)
        menu.findItem(R.id.action_login).isVisible = !loggedIn
        menu.findItem(R.id.action_logout).isVisible = loggedIn
        menu.findItem(R.id.action_submit_grave).isVisible = loggedIn
        menu.findItem(R.id.action_my_submissions).isVisible = loggedIn
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.action_map -> {
                startActivity(Intent(this, MapActivity::class.java))
                return true
            }
            R.id.action_login -> {
                startActivity(Intent(this, LoginActivity::class.java))
                return true
            }
            R.id.action_logout -> {
                com.example.cemeterylocator.api.SessionManager.logout(this)
                Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
                invalidateOptionsMenu()
                return true
            }
            R.id.action_submit_grave -> {
                startActivity(Intent(this, SubmitGraveActivity::class.java))
                return true
            }
            R.id.action_my_submissions -> {
                startActivity(Intent(this, MySubmissionsActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
