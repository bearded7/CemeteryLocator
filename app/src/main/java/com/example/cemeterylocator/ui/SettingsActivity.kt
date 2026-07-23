package com.example.cemeterylocator.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cemeterylocator.api.ApiClient
import com.example.cemeterylocator.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.editBaseUrl.setText(ApiClient.getBaseUrl(this))

        binding.buttonSave.setOnClickListener {
            val url = binding.editBaseUrl.text.toString().trim()
            if (url.isBlank()) {
                Toast.makeText(this, "Enter a server URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            ApiClient.setBaseUrl(this, url)
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
