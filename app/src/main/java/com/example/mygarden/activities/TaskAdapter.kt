package com.example.mygarden.activities

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mygarden.R
import com.example.mygarden.database.Task

class TaskAdapter(
    private var tasks: List<Task>, private val dateSelector: (Task) -> String?,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.taskNameText)
        val dateText: TextView = view.findViewById(R.id.taskDateText)
        val waterText: TextView = view.findViewById(R.id.taskWaterPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.nameText.text = task.name

        val date =dateSelector(task)
        if (date != null) {
            holder.dateText.text = date
            holder.dateText.visibility = View.VISIBLE
        }
        if (task.waterPoints != null) {
            holder.waterText.text = task.waterPoints.toString()
            holder.waterText.visibility = View.VISIBLE
        } else {
            holder.waterText.visibility = View.GONE
        }
        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }

    override fun getItemCount() = tasks.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}