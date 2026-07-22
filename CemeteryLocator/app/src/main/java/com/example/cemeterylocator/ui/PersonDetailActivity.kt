package com.example.cemeterylocator.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.cemeterylocator.api.ApiClient
import com.example.cemeterylocator.databinding.ActivityPersonDetailBinding
import kotlinx.coroutines.launch

class PersonDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val graveNo = intent.getStringExtra(EXTRA_GRAVENO)
        val location = intent.getStringExtra(EXTRA_LOCATION)

        if (graveNo == null) {
            Toast.makeText(this, "Missing grave number", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadPerson(graveNo, location)
    }

    private fun loadPerson(graveNo: String, location: String?) {
        lifecycleScope.launch {
            try {
                val api = ApiClient.create(this@PersonDetailActivity)
                val response = api.getPerson(graveNo, location)
                if (response.isSuccessful && response.body() != null) {
                    val record = response.body()!!
                    supportActionBar?.title = record.name
                    binding.textName.text = record.name
                    binding.textGraveNo.text = "Grave #${record.graveNo}"
                    binding.textSex.text = record.sex ?: "—"
                    binding.textBorn.text = record.bornDate ?: "Unknown"
                    binding.textDied.text = record.diedDate ?: "Unknown"
                    binding.textAge.text = record.age?.toString() ?: "—"
                    binding.textLocation.text = record.location ?: "Unknown"
                    binding.textMemorabilia.text = record.memorabilia ?: ""
                    binding.imagePerson.load(record.photoUrl) { crossfade(true) }
                    binding.imageGrave.load(record.gravePicUrl) { crossfade(true) }

                    if (record.lat != null && record.lng != null) {
                        binding.buttonViewOnMap.visibility = android.view.View.VISIBLE
                        binding.buttonViewOnMap.setOnClickListener {
                            val mapIntent = Intent(this@PersonDetailActivity, MapActivity::class.java)
                            mapIntent.putExtra(MapActivity.EXTRA_FOCUS_LAT, record.lat)
                            mapIntent.putExtra(MapActivity.EXTRA_FOCUS_LNG, record.lng)
                            startActivity(mapIntent)
                        }

                        binding.buttonGetDirections.visibility = android.view.View.VISIBLE
                        binding.buttonGetDirections.setOnClickListener {
                            DirectionsHelper.openExternalDirections(
                                this@PersonDetailActivity, record.lat, record.lng, record.name
                            )
                        }
                    }
                } else {
                    Toast.makeText(
                        this@PersonDetailActivity,
                        "Record not found (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PersonDetailActivity,
                    "Couldn't reach server: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_GRAVENO = "extra_graveno"
        const val EXTRA_LOCATION = "extra_location"
    }
}
