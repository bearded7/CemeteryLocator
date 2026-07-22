package com.example.cemeterylocator.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cemeterylocator.api.ApiClient
import com.example.cemeterylocator.api.SessionManager
import com.example.cemeterylocator.databinding.ActivitySignUpBinding
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.buttonSignUp.setOnClickListener { doSignUp() }
        binding.textGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun doSignUp() {
        val fullName = binding.editFullName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString()
        val confirmPassword = binding.editConfirmPassword.text.toString()

        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonSignUp.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = ApiClient.create(this@SignUpActivity)
                val response = api.register(email, password, fullName)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    SessionManager.saveSession(this@SignUpActivity, body.token, body.user)
                    Toast.makeText(this@SignUpActivity, "Account created", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string()?.let {
                        try {
                            com.google.gson.Gson().fromJson(it, com.example.cemeterylocator.model.ApiError::class.java).error
                        } catch (e: Exception) {
                            null
                        }
                    } ?: "Sign up failed"
                    Toast.makeText(this@SignUpActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SignUpActivity, "Couldn't reach server: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.buttonSignUp.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
