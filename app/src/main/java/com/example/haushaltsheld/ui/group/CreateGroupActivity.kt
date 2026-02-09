package com.example.haushaltsheld.ui.group

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.haushaltsheld.R
import com.example.haushaltsheld.databinding.ActivityCreateGroupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * Simple Activity to create a new group
 * Generates a random invitation code and saves group to Firestore
 */
class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnCreateGroup.setOnClickListener {
            createGroup()
        }
    }

    private fun createGroup() {
        val groupName = binding.etGroupName.text.toString().trim()

        // Simple validation
        if (groupName.isEmpty()) {
            showError(getString(R.string.please_enter_group_name))
            Toast.makeText(this, getString(R.string.please_enter_group_name), Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            showError(getString(R.string.user_not_logged_in))
            Toast.makeText(this, getString(R.string.user_not_logged_in), Toast.LENGTH_SHORT).show()
            return
        }

        // Generate a simple invitation code (6 characters)
        val invitationCode = generateInvitationCode()

        // Show loading
        binding.btnCreateGroup.isEnabled = false
        binding.btnCreateGroup.text = getString(R.string.creating)

        // Create group data
        val groupData = hashMapOf(
            "name" to groupName,
            "invitationCode" to invitationCode,
            "adminId" to currentUser.uid,
            "memberIds" to listOf(currentUser.uid)
        )

        // Save to Firestore
        firestore.collection("groups")
            .add(groupData)
            .addOnSuccessListener { documentReference ->
                // Update user's groupId field (optional, for quick access)
                firestore.collection("users")
                    .document(currentUser.uid)
                    .update("groupId", documentReference.id)
                    .addOnSuccessListener {
                        binding.btnCreateGroup.isEnabled = true
                        binding.btnCreateGroup.text = getString(R.string.create_group)
                        // Show dialog with invitation code
                        showInvitationCodeDialog(invitationCode)
                    }
                    .addOnFailureListener { e ->
                        // Group was created but user update failed - still success
                        android.util.Log.w("CreateGroupActivity", "Group created but user update failed: ${e.message}")
                        binding.btnCreateGroup.isEnabled = true
                        binding.btnCreateGroup.text = getString(R.string.create_group)
                        // Show dialog with invitation code
                        showInvitationCodeDialog(invitationCode)
                    }
            }
            .addOnFailureListener { e ->
                binding.btnCreateGroup.isEnabled = true
                binding.btnCreateGroup.text = getString(R.string.create_group)
                val errorMsg = e.message ?: getString(R.string.failed_to_create_group)
                showError(errorMsg)
                Toast.makeText(this, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                android.util.Log.e("CreateGroupActivity", "Failed to create group", e)
            }
    }

    /**
     * Generate a simple 6-character invitation code
     * Uses uppercase letters and numbers
     */
    private fun generateInvitationCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }

    private fun showInvitationCodeDialog(invitationCode: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.group_created_successfully))
            .setMessage("${getString(R.string.invitation_code)}: $invitationCode\n\n${getString(R.string.share_this_code_with_group_members)}")
            .setPositiveButton(getString(R.string.copy_invitation_code)) { _, _ ->
                // Copy to clipboard
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Invitation Code", invitationCode)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, getString(R.string.invitation_code_copied), Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(getString(R.string.close)) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()
        dialog.show()
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = android.view.View.VISIBLE
    }
}

