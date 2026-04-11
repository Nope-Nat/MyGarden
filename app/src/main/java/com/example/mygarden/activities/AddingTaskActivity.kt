package com.example.mygarden.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import com.example.mygarden.database.Task
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AddingTaskActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addingtask)

        val nameInput = findViewById<EditText>(R.id.TaskName)
        val descInput = findViewById<EditText>(R.id.TaskDescription)
        val dateInput = findViewById<EditText>(R.id.TaskDueDate)
        val addButton = findViewById<Button>(R.id.AddButton)
        val backButton = findViewById<Button>(R.id.BackButton)

        addButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val description = descInput.text.toString().trim()
            val dueDate = dateInput.text.toString().trim()

            if (validateInput(name, dueDate)) {
                saveTask(name, description, dueDate)
            }
        }

        backButton.setOnClickListener { finish() }
    }

    private fun validateInput(name: String, date: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return false
        }

        // Regex for yyyy-mm-dd
        val datePattern = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$")
        if (!datePattern.matcher(date).matches()) {
            Toast.makeText(this, "Use format: yyyy-mm-dd", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun saveTask(name: String, description: String, date: String) {
        val task = Task(name = name, description = description, dueDate = date)

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@AddingTaskActivity)
                db.taskDao().insertTask(task)
                Toast.makeText(this@AddingTaskActivity, "Task saved!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddingTaskActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}