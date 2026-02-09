package com.example.haushaltsheld.model

import java.util.Date

/**
 * Simple data class representing a Task
 * Tasks are household chores assigned to users
 */
data class Task(
    val id: String = "",                    // Firestore document ID
    val groupId: String = "",               // ID of the group this task belongs to
    val title: String = "",                 // Task title (e.g., "Wash dishes")
    val assignedUserId: String = "",        // User ID of the assigned person
    val assignedUserName: String = "",       // Display name of assigned person
    val date: Date = Date(),                // Date when task should be done
    val status: String = "open"             // "open" or "completed"
)

