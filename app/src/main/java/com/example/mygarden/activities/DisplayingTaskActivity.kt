package com.example.mygarden.activities

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.autofill.ContentDataType.Companion.Date
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date

class DisplayingTaskActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_displaying_task)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val taskId = intent.getIntExtra("TASK_ID", -1)
        if (taskId != -1) {
            loadTaskDetails(taskId)
        }

        findViewById<Button>(R.id.BackButton).setOnClickListener {
            finish()
        }
        findViewById<Button>(R.id.DoneButton).setOnClickListener {
            changeTaskDoneUndone(taskId)
            finish()
        }
    }

    private fun loadTaskDetails(taskId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@DisplayingTaskActivity)
            val task = db.taskDao().getTaskById(taskId)

            withContext(Dispatchers.Main) {
                task?.let { loadedTask ->
                    findViewById<TextView>(R.id.displayTaskName).text = loadedTask.name
                    findViewById<TextView>(R.id.displayTaskDescription).text = loadedTask.description
                    findViewById<TextView>(R.id.displayTaskDate).text = loadedTask.dueDate
                }
            }
        }
    }
    @SuppressLint("SimpleDateFormat")
    private fun changeTaskDoneUndone(taskId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@DisplayingTaskActivity)
            val task = db.taskDao().getTaskById(taskId)

            task?.let {
                if (it.done == true) {
                    it.done = false
                }
                else {
                    it.done = true
                    val formatter = SimpleDateFormat("yyyy-MM-dd")
                    val date = Date()
                    it.doneDate = formatter.format(date).toString()
                }
                db.taskDao().updateTask(it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DisplayingTaskActivity, "Changed status!", Toast.LENGTH_SHORT).show()
                    return@withContext
                }
            }
        }
    }
}