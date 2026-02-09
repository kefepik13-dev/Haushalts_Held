package com.example.haushaltsheld.model

/**
 * Simple data class representing a Group
 * Groups are shared households/flats that users can join
 */
data class Group(
    val id: String = "",                    // Firestore document ID
    val name: String = "",                  // Group name (e.g., "Student Flat 5")
    val invitationCode: String = "",        // Unique code to join the group
    val adminId: String = "",               // User ID of the admin
    val memberIds: List<String> = emptyList() // List of all member user IDs
)

