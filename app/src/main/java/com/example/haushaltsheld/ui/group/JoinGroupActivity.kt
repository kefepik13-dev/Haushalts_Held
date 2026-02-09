package com.example.haushaltsheld.ui.group

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.haushaltsheld.R
import com.example.haushaltsheld.databinding.ActivityJoinGroupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Simple Activity to join a group using an invitation code
 * Searches for group by invitation code and adds user to members list
 */
class JoinGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinGroupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnJoinGroup.setOnClickListener {
            joinGroup()
        }
    }

    private fun joinGroup() {
        val invitationCode = binding.etInvitationCode.text.toString().trim().uppercase()

        // Simple validation
        if (invitationCode.isEmpty()) {
            showError(getString(R.string.please_enter_invitation_code))
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            showError(getString(R.string.user_not_logged_in))
            return
        }

        // Show loading
        binding.btnJoinGroup.isEnabled = false

        // Search for group with this invitation code
        firestore.collection("groups")
            .whereEqualTo("invitationCode", invitationCode)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    binding.btnJoinGroup.isEnabled = true
                    showError(getString(R.string.invalid_invitation_code))
                    return@addOnSuccessListener
                }

                // Get the first matching group
                val groupDocument = documents.documents[0]
                val groupId = groupDocument.id
                val memberIds = groupDocument.get("memberIds") as? List<String> ?: emptyList()

                // Check if user is already a member
                if (memberIds.contains(currentUser.uid)) {
                    binding.btnJoinGroup.isEnabled = true
                    showError(getString(R.string.already_member_of_group))
                    return@addOnSuccessListener
                }

                // Add user to members list
                val updatedMemberIds = memberIds + currentUser.uid
                firestore.collection("groups")
                    .document(groupId)
                    .update("memberIds", updatedMemberIds)
                    .addOnSuccessListener {
                        // Update user's groupId field
                        firestore.collection("users")
                            .document(currentUser.uid)
                            .update("groupId", groupId)
                            .addOnCompleteListener {
                                binding.btnJoinGroup.isEnabled = true
                                Toast.makeText(
                                    this,
                                    getString(R.string.joined_group_successfully),
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                    }
                    .addOnFailureListener { e ->
                        binding.btnJoinGroup.isEnabled = true
                        showError(e.message ?: getString(R.string.failed_to_join_group))
                    }
            }
            .addOnFailureListener { e ->
                binding.btnJoinGroup.isEnabled = true
                showError(e.message ?: getString(R.string.failed_to_join_group))
            }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = android.view.View.VISIBLE
    }
}

