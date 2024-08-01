package com.example.todo_app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private var tasks: List<Task> = listOf(),
    private val onTaskClicked: (Task) -> Unit,
    private val onTaskCheckedChanged: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.taskTitleTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.taskDescriptionTextView)
        val dueDateTextView: TextView = view.findViewById(R.id.taskDueDateTextView)
        val completedCheckBox: CheckBox = view.findViewById(R.id.taskCompletedCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.titleTextView.text = task.title
        holder.descriptionTextView.text = task.description
        holder.dueDateTextView.text = formatDate(task.dueDate)
        holder.completedCheckBox.isChecked = task.completed

        holder.itemView.setOnClickListener { onTaskClicked(task) }
        holder.completedCheckBox.setOnCheckedChangeListener { _, isChecked ->
            onTaskCheckedChanged(task, isChecked)
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    private fun formatDate(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
}