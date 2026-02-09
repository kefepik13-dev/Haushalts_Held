package com.example.haushaltsheld.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.haushaltsheld.R
import com.example.haushaltsheld.databinding.ActivityRegisterBinding
import com.example.haushaltsheld.ui.dashboard.DashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Simple Register Activity
 * Handles user registration using Firebase Auth
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Register button click
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        // Login link click
        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        // Simple validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError(getString(R.string.please_fill_all_fields))
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.invalid_email))
            return
        }

        if (password.length < 6) {
            showError(getString(R.string.password_too_short))
            return
        }

        if (password != confirmPassword) {
            showError(getString(R.string.passwords_do_not_match))
            return
        }

        // Show loading
        binding.btnRegister.isEnabled = false

        // Create user with Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful, save user data to Firestore
                    val user = auth.currentUser
                    if (user != null) {
                        saveUserToFirestore(user.uid, name, email)
                    }
                } else {
                    // Registration failed
                    binding.btnRegister.isEnabled = true
                    showError(task.exception?.message ?: getString(R.string.registration_failed))
                }
            }
    }

    private fun saveUserToFirestore(uid: String, name: String, email: String) {
        val userData = hashMapOf(
            "id" to uid,
            "email" to email,
            "name" to name
        )

        firestore.collection("users")
            .document(uid)
            .set(userData)
            .addOnCompleteListener { task ->
                binding.btnRegister.isEnabled = true

                if (task.isSuccessful) {
                    // Navigate to dashboard - registration successful
                    Toast.makeText(this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
                    navigateToDashboard()
                } else {
                    // Even if Firestore fails, user is registered in Auth, so navigate anyway
                    // But show a warning
                    android.util.Log.w("RegisterActivity", "Failed to save user to Firestore: ${task.exception?.message}")
                    Toast.makeText(this, "Registration successful, but user data could not be saved", Toast.LENGTH_LONG).show()
                    navigateToDashboard()
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

