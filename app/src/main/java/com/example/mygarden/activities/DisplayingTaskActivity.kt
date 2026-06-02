package com.example.mygarden.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import com.example.mygarden.database.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Date

class DisplayingTaskActivity : AppCompatActivity() {

    // --- STATE VARIABLES --- //
    private var voiceNotePlayer: MediaPlayer? = null
    private var currentTask: Task? = null
    private var isDeleted = false

    // --- LIFECYCLE --- //
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_displaying_task)

        setupWindowInsets()
        setupListeners()

        val taskId = intent.getIntExtra("TASK_ID", -1)
        if (taskId != -1) {
            loadTaskDetails(taskId)
        }
    }

    override fun onStop() {
        super.onStop()
        voiceNotePlayer?.release()
        voiceNotePlayer = null

        if (!isDeleted) {
            currentTask?.let { task ->
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(this@DisplayingTaskActivity)
                    db.taskDao().updateTask(task)
                }
            }
        }
    }

    // --- SETUP METHODS --- //
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupListeners() {
        findViewById<Button>(R.id.BackButton).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.DoneButton).setOnClickListener {
            val taskId = intent.getIntExtra("TASK_ID", -1)
            if (taskId != -1) {
                changeTaskDoneUndone(taskId)
            }
        }

        // NOWY KOD DO EDYCJI
        findViewById<Button>(R.id.EditButton).setOnClickListener {
            val taskId = intent.getIntExtra("TASK_ID", -1)
            if (taskId != -1) {
                val editIntent = Intent(this, AddingTaskActivity::class.java)
                editIntent.putExtra("EDIT_TASK_ID", taskId)
                startActivity(editIntent)
                finish()
            }
        }

        findViewById<Button>(R.id.DeleteButton).setOnClickListener {
            val taskId = intent.getIntExtra("TASK_ID", -1)
            if (taskId != -1) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Confirm Deletion")
                    .setMessage("Are you sure you want to delete this? This cannot be undone.")
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton("Yes, Wipe") { _, _ ->
                        isDeleted = true
                        lifecycleScope.launch(Dispatchers.IO) {
                            val db = AppDatabase.getDatabase(this@DisplayingTaskActivity)
                            db.taskDao().deleteTask(taskId)
                            withContext(Dispatchers.Main) {
                                finish()
                            }
                        }
                    }
                    .show()
            }
        }
    }

    // --- LOGIC METHODS --- //
    private fun loadTaskDetails(taskId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@DisplayingTaskActivity)
            val task = db.taskDao().getTaskById(taskId)

            val parentTask = task?.parentId?.let { db.taskDao().getTaskById(it) }
            val subtasks = db.taskDao().getSubtasks(taskId)

            withContext(Dispatchers.Main) {
                task?.let { loadedTask ->
                    currentTask = loadedTask

                    findViewById<TextView>(R.id.displayTaskName).text = loadedTask.name

                    val descriptionView = findViewById<TextView>(R.id.displayTaskDescription)
                    descriptionView.text = loadedTask.description
                    descriptionView.visibility = if (!loadedTask.description.isNullOrEmpty()) View.VISIBLE else View.GONE

                    val dateView = findViewById<TextView>(R.id.displayTaskDate)
                    dateView.text = loadedTask.dueDate
                    dateView.visibility = if (!loadedTask.dueDate.isNullOrEmpty()) View.VISIBLE else View.GONE

                    val waterView = findViewById<TextView>(R.id.displayWaterPoints)
                    if (loadedTask.waterPoints != null && loadedTask.waterPoints!! > 0) {
                        waterView.text = loadedTask.waterPoints.toString()
                        waterView.visibility = View.VISIBLE
                    } else {
                        waterView.visibility = View.GONE
                    }

                    val btnParent = findViewById<Button>(R.id.btnGoToParent)
                    if (parentTask != null) {
                        btnParent.visibility = View.VISIBLE
                        btnParent.text = "PARENT: ${parentTask.name}"
                        btnParent.setOnClickListener {
                            val intent = Intent(this@DisplayingTaskActivity, DisplayingTaskActivity::class.java)
                            intent.putExtra("TASK_ID", parentTask.id)
                            startActivity(intent)
                        }
                    } else {
                        btnParent.visibility = View.GONE
                    }

                    val btnSubtasks = findViewById<Button>(R.id.btnViewSubtasks)
                    if (subtasks.isNotEmpty()) {
                        btnSubtasks.visibility = View.VISIBLE
                        btnSubtasks.text = "SUBTASK (${subtasks.size})"
                        btnSubtasks.setOnClickListener {
                            val names = subtasks.map { it.name }.toTypedArray()
                            MaterialAlertDialogBuilder(this@DisplayingTaskActivity)
                                .setTitle("Select Subtask")
                                .setItems(names) { _, which ->
                                    val selectedSubtask = subtasks[which]
                                    val intent = Intent(this@DisplayingTaskActivity, DisplayingTaskActivity::class.java)
                                    intent.putExtra("TASK_ID", selectedSubtask.id)
                                    startActivity(intent)
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    } else {
                        btnSubtasks.visibility = View.GONE
                    }

                    val progressHeader = findViewById<TextView>(R.id.displayProgressHeader)
                    val progressBar = findViewById<SeekBar>(R.id.taskProgressBar)

                    progressBar.progress = loadedTask.progress
                    progressHeader.text = "Progress: ${loadedTask.progress}%"

                    progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            progressHeader.text = "Progress: $progress%"
                            currentTask?.progress = progress
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })

                    // --- LOCATION --- //
                    val addrView = findViewById<TextView>(R.id.displayTaskAddress)
                    val coordView = findViewById<TextView>(R.id.displayTaskCoords)

                    if (!loadedTask.address.isNullOrEmpty()) {
                        addrView.visibility = View.VISIBLE
                        addrView.text = "📍 ${loadedTask.address}"

                        if (loadedTask.latitude != null && loadedTask.longitude != null) {
                            coordView.visibility = View.VISIBLE
                            coordView.text = "Coordinates: ${loadedTask.latitude}, ${loadedTask.longitude}"
                        } else {
                            coordView.visibility = View.GONE
                        }
                    } else {
                        addrView.visibility = View.GONE
                        coordView.visibility = View.GONE
                    }

                    // --- PHOTO --- //
                    val photoHeaderView = findViewById<TextView>(R.id.displayTaskPhotoHeader)
                    val photoView = findViewById<ImageView>(R.id.displayTaskPhoto)

                    if (!loadedTask.photo.isNullOrEmpty()) {
                        photoHeaderView.visibility = View.VISIBLE
                        photoView.setImageURI(loadedTask.photo.toUri())
                        photoView.visibility = View.GONE

                        var isPhotoVisible = false
                        photoHeaderView.setOnClickListener {
                            isPhotoVisible = !isPhotoVisible
                            photoView.visibility = if (isPhotoVisible) View.VISIBLE else View.GONE
                            photoHeaderView.text = if (isPhotoVisible) "Hide Photo" else "Show Photo"
                        }
                    } else {
                        photoHeaderView.visibility = View.GONE
                        photoView.visibility = View.GONE
                    }

                    // --- HANDWRITTEN NOTE --- //
                    val handwrittenHeaderView = findViewById<TextView>(R.id.displayTaskHandwrittenHeader)
                    val handwrittenView = findViewById<ImageView>(R.id.displayTaskHandwritten)

                    if (!loadedTask.handwrittenPhoto.isNullOrEmpty()) {
                        handwrittenHeaderView.visibility = View.VISIBLE
                        handwrittenView.setImageURI(loadedTask.handwrittenPhoto.toUri())
                        handwrittenView.visibility = View.GONE

                        var isHwVisible = false
                        handwrittenHeaderView.setOnClickListener {
                            isHwVisible = !isHwVisible
                            handwrittenView.visibility = if (isHwVisible) View.VISIBLE else View.GONE
                            handwrittenHeaderView.text = if (isHwVisible) "Hide Handwritten Note" else "Show Handwritten Note"
                        }
                    } else {
                        handwrittenHeaderView.visibility = View.GONE
                        handwrittenView.visibility = View.GONE
                    }

                    // --- VOICE NOTE --- //
                    val voiceNoteButton = findViewById<TextView>(R.id.displayVoiceNoteButton)

                    if (!loadedTask.voiceNote.isNullOrEmpty()) {
                        voiceNoteButton.visibility = View.VISIBLE
                        voiceNoteButton.setOnClickListener {
                            playVoiceNote(loadedTask.voiceNote, voiceNoteButton)
                        }
                    } else {
                        voiceNoteButton.visibility = View.GONE
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SimpleDateFormat")
    private fun changeTaskDoneUndone(taskId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@DisplayingTaskActivity)
            val task = currentTask ?: db.taskDao().getTaskById(taskId)

            task?.let { loadedTask ->
                // --- REVERT TO UNDONE --- //
                if (loadedTask.done) {
                    loadedTask.done = false
                    loadedTask.doneDate = null
                    db.taskDao().updateTask(loadedTask)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DisplayingTaskActivity, "Changed to undone!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                // --- LOCATION VALIDATION --- //
                if (loadedTask.latitude != null && loadedTask.longitude != null) {
                    val isLocationValid = validateLocation(loadedTask.latitude, loadedTask.longitude)

                    if (!isLocationValid) {
                        return@launch
                    }
                }

                // --- MARK AS DONE --- //
                val state = db.globalStateDao().getState(1)
                val pointsToAdd = calculatePointsToAward(loadedTask.dueDate, loadedTask.waterPoints ?: 0)
                state.waterPoint += pointsToAdd

                loadedTask.waterPoints = 0
                loadedTask.done = true
                val formatter = SimpleDateFormat("yyyy-MM-dd")
                loadedTask.doneDate = formatter.format(Date()).toString()

                loadedTask.progress = 100
                db.taskDao().updateTask(loadedTask)
                db.globalStateDao().updateState(state)

                withContext(Dispatchers.Main) {
                    if (state.soundOn) {
                        playDoneSound()
                    }
                    Toast.makeText(this@DisplayingTaskActivity, "Task done!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    // --- EXTRAS --- //
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun calculatePointsToAward(dueDateStr: String?, originalPoints: Int): Int {
        if (originalPoints <= 0) return 0

        if (!dueDateStr.isNullOrEmpty()) {
            try {
                val dueDate = LocalDate.parse(dueDateStr)
                val today = LocalDate.now()
                if (!dueDate.isBefore(today)) {
                    return originalPoints
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DisplayingTaskActivity, "Task overdue! No water drops awarded.", Toast.LENGTH_LONG).show()
                    }
                    return 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return originalPoints
            }
        }
        return originalPoints
    }

    private suspend fun validateLocation(taskLatitude: Double, taskLongitude: Double): Boolean {
        val hasPermission = ActivityCompat.checkSelfPermission(this@DisplayingTaskActivity,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DisplayingTaskActivity, "Permit location!", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val lastLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastLoc != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                lastLoc.latitude, lastLoc.longitude,
                taskLatitude, taskLongitude,
                results
            )
            val distanceInMeters = results[0]

            if (distanceInMeters > 250) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DisplayingTaskActivity, "Too far (${distanceInMeters.toInt()}m), stop lying!", Toast.LENGTH_LONG).show()
                }
                return false
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DisplayingTaskActivity, "Unable to check location, check GPS.", Toast.LENGTH_LONG).show()
            }
            return false
        }
        return true
    }

    private fun playDoneSound() {
        val player = MediaPlayer.create(applicationContext, R.raw.done_sound_effect)
        player?.setOnCompletionListener {
            it.release()
        }
        player?.start()
    }

    private fun playVoiceNote(path: String, button: TextView) {
        if (voiceNotePlayer?.isPlaying == true) return

        voiceNotePlayer = MediaPlayer().apply {
            try {
                setDataSource(path)
                prepare()
                start()

                button.isEnabled = false
                button.text = "Playing..."

                setOnCompletionListener {
                    release()
                    voiceNotePlayer = null
                    button.isEnabled = true
                    button.text = "Play Voice Note"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@DisplayingTaskActivity, "Error playing audio", Toast.LENGTH_SHORT).show()
            }
        }
    }
}