package com.example.haushaltsheld.model

/**
 * Simple data class representing a User
 * Used to store user information in Firestore
 */
data class User(
    val id: String = "",           // Firebase Auth UID
    val email: String = "",
    val name: String = ""          // Display name
)

