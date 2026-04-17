package com.example.mygarden.activities

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import com.example.mygarden.database.Task
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class AddingTaskActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addingtask)

        val nameInput = findViewById<EditText>(R.id.TaskName)
        val descInput = findViewById<EditText>(R.id.TaskDescription)
        val dateInput = findViewById<EditText>(R.id.TaskDueDate)

        findViewById<Button>(R.id.AddButton).setOnClickListener {
            val name = nameInput.text.toString().trim()
            val description = descInput.text.toString().trim()
            val dueDate = dateInput.text.toString().trim()

            if (validateInput(name, dueDate)) {
                saveTask(name, description, dueDate, savedPhotoPath)
            }
        }

        findViewById<Button>(R.id.BackButton).setOnClickListener {
            finish()
        }
        findViewById<Button>(R.id.PhotoButton).setOnClickListener {
            val uri=createImageUri()
            if (uri!=null){
                currentPhotoUri=uri
                takePictureLauncher.launch(uri)
            } else {
                Toast.makeText(this, "Error while creating file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInput(name: String, date: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return false
        }

        val datePattern = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$")
        if (!date.isEmpty() && !datePattern.matcher(date).matches()) {
            Toast.makeText(this, "Use format: yyyy-mm-dd", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm =
                getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun saveTask(name: String, description: String, date: String, photoPath: String?) {
        val date = if (date.isEmpty()) null else date
        val task = Task(name = name, description = description, dueDate = date, photo = photoPath)

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@AddingTaskActivity)
                db.taskDao().insertTask(task)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(this@AddingTaskActivity, "Task saved!", Toast.LENGTH_SHORT)
                        .show()
                    hideKeyboard()
                    finish()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(
                        this@AddingTaskActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // --- PHOTO --- //
    private var currentPhotoUri: Uri? = null
    private var savedPhotoPath: String? = null

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success){
                savedPhotoPath = currentPhotoUri.toString()
                Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                currentPhotoUri = null
                savedPhotoPath = null
            }
        }
    private fun createImageUri() : Uri? {
        return try {
            val directory = File(cacheDir, "camera_image").apply {mkdirs()}
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "MG_${timeStamp}.jpg"
            val file = File(directory, fileName)
            FileProvider.getUriForFile(this, "${packageName}.provider", file)
        } catch (e: Exception){
            e.printStackTrace()
            null
        }
    }
}