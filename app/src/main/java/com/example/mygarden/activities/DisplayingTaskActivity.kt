package com.example.mygarden.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class DisplayingTaskActivity : BaseActivity() {

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
        }
    }

    private fun loadTaskDetails(taskId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@DisplayingTaskActivity)
            val task = db.taskDao().getTaskById(taskId)

            withContext(Dispatchers.Main) {
                task?.let { loadedTask ->
                    findViewById<TextView>(R.id.displayTaskName).text = loadedTask.name

                    val descriptionView=findViewById<TextView>(R.id.displayTaskDescription)
                    descriptionView.text = loadedTask.description
                    if (!loadedTask.description.isNullOrEmpty()) {
                        descriptionView.visibility = View.VISIBLE
                    }
                    else {
                        descriptionView.visibility = View.GONE
                    }

                    val dateView=findViewById<TextView>(R.id.displayTaskDate)
                    dateView.text = loadedTask.dueDate
                    if (!loadedTask.dueDate.isNullOrEmpty()) {
                        dateView.visibility = View.VISIBLE
                    }
                    else {
                        dateView.visibility = View.GONE
                    }

                    val addrView = findViewById<TextView>(R.id.displayTaskAddress)
                    val coordView = findViewById<TextView>(R.id.displayTaskCoords)

                    if (!loadedTask.address.isNullOrEmpty()) {
                        addrView.visibility = View.VISIBLE
                        addrView.text = "📍 ${loadedTask.address}"

                        if (loadedTask.latitude != null && loadedTask.longitude != null) {
                            coordView.visibility = View.VISIBLE
                            coordView.text = "Coordinates: ${loadedTask.latitude}, ${loadedTask.longitude}"
                        }
                    } else {
                        addrView.visibility = View.GONE
                        coordView.visibility = View.GONE
                    }

                    val photoHeaderView = findViewById<TextView>(R.id.displayTaskPhotoHeader)
                    val photoView = findViewById<ImageView>(R.id.displayTaskPhoto)

                    if (!loadedTask.photo.isNullOrEmpty()) {
                        photoHeaderView.visibility = View.VISIBLE
                        photoView.setImageURI(loadedTask.photo.toUri())

                        photoView.visibility = View.GONE
                        var isPhotoVisible = false
                        photoHeaderView.setOnClickListener {
                            isPhotoVisible = !isPhotoVisible
                            if (isPhotoVisible) {
                                photoView.visibility = View.VISIBLE
                                photoHeaderView.text = "Hide Photo"
                            } else {
                                photoView.visibility = View.GONE
                                photoHeaderView.text = "Show Photo"
                            }
                        }
                    } else {
                        photoHeaderView.visibility = View.GONE
                        photoView.visibility = View.GONE
                    }

                    val handwrittenHeaderView = findViewById<TextView>(R.id.displayTaskHandwrittenHeader)
                    val handwrittenView = findViewById<ImageView>(R.id.displayTaskHandwritten)

                    if (!loadedTask.handwrittenPhoto.isNullOrEmpty()) {
                        handwrittenHeaderView.visibility = View.VISIBLE
                        handwrittenView.setImageURI(loadedTask.handwrittenPhoto.toUri())

                        handwrittenView.visibility = View.GONE
                        var isPhotoVisible = false
                        handwrittenHeaderView.setOnClickListener {
                            isPhotoVisible = !isPhotoVisible
                            if (isPhotoVisible) {
                                handwrittenView.visibility = View.VISIBLE
                                handwrittenHeaderView.text = "Hide Handwritten Note"
                            } else {
                                handwrittenView.visibility = View.GONE
                                handwrittenHeaderView.text = "Show Handwritten Note"
                            }
                        }
                    } else {
                        handwrittenHeaderView.visibility = View.GONE
                        handwrittenView.visibility = View.GONE
                    }
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
                    it.doneDate = null

                    db.taskDao().updateTask(it)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DisplayingTaskActivity, "Changed to undone!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                if (it.latitude != null && it.longitude != null) {
                    val hasPermission = ActivityCompat.checkSelfPermission(
                        this@DisplayingTaskActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@DisplayingTaskActivity, "Permit location!", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                    val lastLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                    if (lastLoc != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            lastLoc.latitude, lastLoc.longitude,
                            it.latitude, it.longitude,
                            results
                        )
                        val distanceInMeters = results[0]

                        if (distanceInMeters > 500) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@DisplayingTaskActivity, "Too far (${distanceInMeters.toInt()}m), stop lying!", Toast.LENGTH_LONG).show()
                            }
                            return@launch
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@DisplayingTaskActivity, "Unable to check location, check GPS.", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                }

                it.done = true
                val formatter = SimpleDateFormat("yyyy-MM-dd")
                it.doneDate = formatter.format(Date()).toString()
                db.taskDao().updateTask(it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DisplayingTaskActivity, "Task done!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}