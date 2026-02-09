package com.example.haushaltsheld.ui.task

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.haushaltsheld.R
import com.example.haushaltsheld.model.Task
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView Adapter for displaying tasks.
 * Shows task title, assigned user, date, status. Optional: left border color by user.
 */
class TaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val userIdToColorMap: Map<String, Int>? = null
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view, userIdToColorMap)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    class TaskViewHolder(
        itemView: View,
        private val userIdToColorMap: Map<String, Int>?
    ) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.taskCard)
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

            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            tvDate.text = dateFormat.format(task.date)

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

            // Color border by assigned user
            val color = userIdToColorMap?.get(task.assignedUserId)
            if (color != null) {
                cardView.strokeColor = color
                cardView.strokeWidth = (itemView.context.resources.displayMetrics.density * 4).toInt()
            } else {
                cardView.strokeWidth = 0
            }
        }
    }
}

