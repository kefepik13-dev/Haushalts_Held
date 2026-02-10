package com.example.haushaltsheld.ui.dashboard

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.haushaltsheld.R
import com.example.haushaltsheld.databinding.ActivityDashboardBinding
import com.example.haushaltsheld.databinding.DialogAddTaskBinding
import com.example.haushaltsheld.model.Task
import com.example.haushaltsheld.model.User
import com.example.haushaltsheld.repository.FirebaseUserRepository
import com.example.haushaltsheld.repository.UserRepository
import com.example.haushaltsheld.ui.auth.LoginActivity
import com.example.haushaltsheld.ui.calendar.CalendarActivity
import com.example.haushaltsheld.ui.group.CreateGroupActivity
import com.example.haushaltsheld.ui.group.JoinGroupActivity
import com.example.haushaltsheld.ui.task.TaskAdapter
import com.example.haushaltsheld.ui.task.UserAutoCompleteAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main Dashboard Activity
 * Shows tasks assigned to the user and all group tasks
 * Allows creating new tasks and managing groups
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var userRepository: UserRepository
    private var currentGroupId: String? = null
    private var currentUserId: String? = null
    private var showingMyTasks = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userRepository = FirebaseUserRepository(firestore)

        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        currentUserId = currentUser.uid

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        setupTabs()
        loadUserGroup()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(emptyList(), { task ->
            // Handle task click - toggle completion status
            toggleTaskCompletion(task)
        })
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = taskAdapter
    }

    private fun setupClickListeners() {
        // Create Group button
        binding.btnCreateGroup.setOnClickListener {
            startActivity(Intent(this, CreateGroupActivity::class.java))
        }

        // Join Group button
        binding.btnJoinGroup.setOnClickListener {
            startActivity(Intent(this, JoinGroupActivity::class.java))
        }

        // Add Task FAB
        binding.btnAddTask.setOnClickListener {
            showAddTaskDialog()
        }

        // Calendar button
        binding.btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        showingMyTasks = true
                        loadTasks()
                    }
                    1 -> {
                        showingMyTasks = false
                        loadTasks()
                    }
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun loadUserGroup() {
        val currentUser = auth.currentUser ?: return

        // Get user's groupId from Firestore
        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                currentGroupId = document.getString("groupId")
                if (currentGroupId != null) {
                    loadGroupInfo()
                    loadTasks()
                } else {
                    // User has no group, hide group info card
                    binding.cardGroupInfo.visibility = android.view.View.GONE
                    // Show message
                    Toast.makeText(
                        this,
                        getString(R.string.please_create_or_join_group),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun loadGroupInfo() {
        val groupId = currentGroupId ?: return

        // Load group information
        firestore.collection("groups")
            .document(groupId)
            .get()
            .addOnSuccessListener { document ->
                val groupName = document.getString("name") ?: ""
                val invitationCode = document.getString("invitationCode") ?: ""

                // Show group info card
                binding.cardGroupInfo.visibility = android.view.View.VISIBLE
                binding.tvGroupName.text = groupName
                binding.tvInvitationCode.text = invitationCode

                // Copy invitation code button
                binding.btnCopyInvitationCode.setOnClickListener {
                    val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Invitation Code", invitationCode)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, getString(R.string.invitation_code_copied), Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DashboardActivity", "Failed to load group info", e)
            }
    }

    private fun loadTasks() {
        val groupId = currentGroupId ?: return

        // Load tasks from Firestore
        firestore.collection("tasks")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { documents ->
                val tasks = documents.map { doc ->
                    val data = doc.data
                    Task(
                        id = doc.id,
                        groupId = data["groupId"] as? String ?: "",
                        title = data["title"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        assignedUserId = data["assignedUserId"] as? String ?: "",
                        assignedUserName = data["assignedUserName"] as? String ?: "",
                        date = (data["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                        status = data["status"] as? String ?: "open"
                    )
                }.sortedBy { it.date }

                // Filter tasks based on tab selection
                val filteredTasks = if (showingMyTasks) {
                    tasks.filter { it.assignedUserId == currentUserId }
                } else {
                    tasks
                }

                taskAdapter = TaskAdapter(filteredTasks, { task ->
                    toggleTaskCompletion(task)
                })
                binding.rvTasks.adapter = taskAdapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun showAddTaskDialog() {
        val groupId = currentGroupId
        if (groupId == null) {
            Toast.makeText(
                this,
                getString(R.string.please_create_or_join_group),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        var selectedDate = Date()
        var selectedUser: User? = null

        // Update date display
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        dialogBinding.tvSelectedDate.text = dateFormat.format(selectedDate)

        // Load users from group and populate AutoCompleteTextView
        dialogBinding.tilAssignedUser.isEnabled = false
        userRepository.loadUsersByGroup(groupId) { result ->
            result.fold(
                onSuccess = { users ->
                    if (users.isEmpty()) {
                        Toast.makeText(
                            this@DashboardActivity,
                            getString(R.string.no_users_in_group),
                            Toast.LENGTH_SHORT
                        ).show()
                        dialogBinding.tilAssignedUser.isEnabled = false
                    } else {
                        val adapter = UserAutoCompleteAdapter(this@DashboardActivity, users)
                        dialogBinding.autoCompleteUser.setAdapter(adapter)
                        dialogBinding.tilAssignedUser.isEnabled = true

                        // Handle user selection: map displayed name to stored email/ID
                        dialogBinding.autoCompleteUser.setOnItemClickListener { _, _, position, _ ->
                            selectedUser = adapter.getItem(position)
                        }
                    }
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@DashboardActivity,
                        getString(R.string.error_loading_users),
                        Toast.LENGTH_SHORT
                    ).show()
                    android.util.Log.e("DashboardActivity", "Error loading users", exception)
                }
            )
        }

        // Date picker
        dialogBinding.btnSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                    dialogBinding.tvSelectedDate.text = dateFormat.format(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Cancel button
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Add button
        dialogBinding.btnAdd.setOnClickListener {
            val title = dialogBinding.etTaskTitle.text.toString().trim()
            val description = dialogBinding.etTaskDescription.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, getString(R.string.please_enter_task_title), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (selectedUser == null) {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_user),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Use selected user's ID and name (stored internally, displayed name was shown in UI)
            val assignedUserId = selectedUser!!.id
            val assignedUserName = selectedUser!!.name.ifEmpty { selectedUser!!.email }

            // Create task
            val taskData = hashMapOf(
                "groupId" to groupId,
                "title" to title,
                "description" to description,
                "assignedUserId" to assignedUserId,
                "assignedUserName" to assignedUserName,
                "date" to com.google.firebase.Timestamp(selectedDate),
                "status" to "open"
            )

            firestore.collection("tasks")
                .add(taskData)
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        getString(R.string.task_added_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                    loadTasks()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        dialog.show()
    }

    private fun toggleTaskCompletion(task: Task) {
        val newStatus = if (task.status == "open") "completed" else "open"

        firestore.collection("tasks")
            .document(task.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                loadTasks()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logout() {
        auth.signOut()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Reload group info and tasks when returning to this activity
        loadUserGroup()
    }
}

