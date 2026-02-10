package com.example.haushaltsheld.repository

import com.example.haushaltsheld.model.User
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Repository interface for user data operations (Clean Architecture).
 * Abstracts the data source (Firebase, REST API, Room, etc.).
 */
interface UserRepository {
    /**
     * Load all users that belong to the same group.
     * @param groupId The group ID to filter users by
     * @param callback Called with the list of users or an error
     */
    fun loadUsersByGroup(groupId: String, callback: (Result<List<User>>) -> Unit)
}

/**
 * Firebase implementation of UserRepository.
 */
class FirebaseUserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override fun loadUsersByGroup(groupId: String, callback: (Result<List<User>>) -> Unit) {
        firestore.collection("users")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { documents ->
                val users = documents.map { doc ->
                    User(
                        id = doc.id,
                        email = doc.getString("email") ?: "",
                        name = doc.getString("name") ?: ""
                    )
                }
                callback(Result.success(users))
            }
            .addOnFailureListener { exception ->
                callback(Result.failure(exception))
            }
    }
}
