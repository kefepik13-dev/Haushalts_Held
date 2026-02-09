package com.example.haushaltsheld.ui.task

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.haushaltsheld.R
import com.example.haushaltsheld.model.Task
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simple RecyclerView Adapter for displaying tasks
 * Shows task title, assigned user, date, and status
 */
class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        private val tvAssignedUser: TextView = itemView.findViewById(R.id.tvAssignedUser)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(task: Task) {
            tvTaskTitle.text = task.title
            tvAssignedUser.text = itemView.context.getString(
                R.string.assigned_to,
                task.assignedUserName
            )

            // Format date
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            tvDate.text = dateFormat.format(task.date)

            // Set status
            tvStatus.text = task.status.uppercase()
            if (task.status == "completed") {
                tvStatus.setBackgroundColor(
                    itemView.context.getColor(android.R.color.holo_green_light)
                )
            } else {
                tvStatus.setBackgroundColor(
                    itemView.context.getColor(android.R.color.holo_blue_light)
                )
            }
        }
    }
}

