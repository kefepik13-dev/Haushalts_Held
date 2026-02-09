package com.example.haushaltsheld.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.haushaltsheld.R
import com.example.haushaltsheld.databinding.ActivityLoginBinding
import com.example.haushaltsheld.ui.dashboard.DashboardActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Simple Login Activity
 * Handles user authentication using Firebase Auth
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()

            // Check if user is already logged in
            if (auth.currentUser != null) {
                navigateToDashboard()
                return
            }

            setupClickListeners()
        } catch (e: Exception) {
            // Show error if something goes wrong
            android.util.Log.e("LoginActivity", "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            
            // Fallback: Show simple error message
            android.widget.TextView(this).apply {
                text = "Error loading layout: ${e.message}"
                setPadding(50, 50, 50, 50)
                setContentView(this)
            }
        }
    }

    private fun setupClickListeners() {
        // Login button click
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        // Register link click
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        // Simple validation
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.please_fill_all_fields))
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.invalid_email))
            return
        }

        // Show loading (you can add a progress bar here)
        binding.btnLogin.isEnabled = false

        // Sign in with Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    // Login successful
                    navigateToDashboard()
                } else {
                    // Login failed
                    showError(task.exception?.message ?: getString(R.string.login_failed))
                }
            }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = android.view.View.VISIBLE
    }
}

