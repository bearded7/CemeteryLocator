package com.example.cemeterylocator.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cemeterylocator.api.ApiClient
import com.example.cemeterylocator.databinding.ActivityMySubmissionsBinding
import kotlinx.coroutines.launch

class MySubmissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMySubmissionsBinding
    private lateinit var adapter: SubmissionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMySubmissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = SubmissionAdapter()
        binding.recyclerSubmissions.layoutManager = LinearLayoutManager(this)
        binding.recyclerSubmissions.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { load() }
        load()
    }

    private fun load() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val api = ApiClient.create(this@MySubmissionsActivity)
                val response = api.mySubmissions()
                if (response.isSuccessful) {
                    val list = response.body()?.submissions.orEmpty()
                    adapter.submitList(list)
                    binding.textEmpty.visibility =
                        if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                } else {
                    Toast.makeText(this@MySubmissionsActivity, "Error ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MySubmissionsActivity, "Couldn't reach server: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
