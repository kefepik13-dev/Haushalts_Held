package com.example.haushaltsheld.ui.calendar

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.haushaltsheld.R
import com.example.haushaltsheld.databinding.ActivityCalendarBinding
import com.example.haushaltsheld.model.Task
import com.example.haushaltsheld.ui.task.TaskAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple Calendar Activity
 * Shows tasks for a selected date
 * Uses DatePicker to select dates
 */
class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var taskAdapter: TaskAdapter
    private var currentGroupId: String? = null
    private var selectedDate = Date()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadUserGroup()
        updateDateDisplay()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(emptyList()) { task ->
            // Handle task click - toggle completion status
            toggleTaskCompletion(task)
        }
        binding.rvTasks.layoutManager = LinearLayoutManager(this)
        binding.rvTasks.adapter = taskAdapter
    }

    private fun setupClickListeners() {
        // Date picker button
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
                updateDateDisplay()
                loadTasksForDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = dateFormat.format(selectedDate)
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
                    loadTasksForDate()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.please_create_or_join_group),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun loadTasksForDate() {
        val groupId = currentGroupId ?: return

        // Calculate start and end of selected date
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.time

        val startTimestamp = com.google.firebase.Timestamp(startOfDay)
        val endTimestamp = com.google.firebase.Timestamp(endOfDay)

        // Load tasks for the selected date
        firestore.collection("tasks")
            .whereEqualTo("groupId", groupId)
            .whereGreaterThanOrEqualTo("date", startTimestamp)
            .whereLessThan("date", endTimestamp)
            .get()
            .addOnSuccessListener { documents ->
                val tasks = documents.map { doc ->
                    val data = doc.data
                    Task(
                        id = doc.id,
                        groupId = data["groupId"] as? String ?: "",
                        title = data["title"] as? String ?: "",
                        assignedUserId = data["assignedUserId"] as? String ?: "",
                        assignedUserName = data["assignedUserName"] as? String ?: "",
                        date = (data["date"] as? com.google.firebase.Timestamp)?.toDate() ?: Date(),
                        status = data["status"] as? String ?: "open"
                    )
                }.sortedBy { it.date }

                taskAdapter = TaskAdapter(tasks) { task ->
                    toggleTaskCompletion(task)
                }
                binding.rvTasks.adapter = taskAdapter

                if (tasks.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.no_tasks_for_this_date),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun toggleTaskCompletion(task: Task) {
        val newStatus = if (task.status == "open") "completed" else "open"

        firestore.collection("tasks")
            .document(task.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                loadTasksForDate()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

