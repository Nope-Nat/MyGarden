package com.example.mygarden.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import com.example.mygarden.database.Task
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.util.*

class AddingTaskActivity : BaseActivity() {

    // --- STATE VARIABLES --- //

    // Location
    private var searchJob: Job? = null
    private val locationDataMap = mutableMapOf<String, Pair<Double, Double>>()
    private lateinit var locationAdapter: ArrayAdapter<String>
    private var selectedLat: Double? = null
    private var selectedLon: Double? = null
    // Photos
    private var currentPhotoUri: Uri? = null
    private var savedPhotoPath: String? = null
    // Touchpad
    private var currentHandwritingPath: String? = null
    // Microphone
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentVoicePath: String? = null
    private var isRecording = false
    private var hasRecorded = false

    // --- ACTIVITY RESULT LAUNCHERS (PERMISSIONS & INTENTS) --- //

    private val requestMicPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            handleAudioAction()
        } else {
            Toast.makeText(this, "Permit microphone!", Toast.LENGTH_SHORT).show()
        }
    }
    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Permit camera!", Toast.LENGTH_SHORT).show()
        }
    }
    private val requestLocationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permit location!", Toast.LENGTH_SHORT).show()
        }
    }
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            savedPhotoPath = currentPhotoUri.toString()
            val photoPreview = findViewById<ImageView>(R.id.PhotoPreview)
            val photoButton = findViewById<Button>(R.id.PhotoButton)

            photoPreview.visibility = View.VISIBLE
            photoPreview.setImageURI(currentPhotoUri)
            photoButton.visibility = View.GONE
        }
    }

    // --- LIFECYCLE --- //
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addingtask)

        setupCalendar()
        setupLocation()
        setupCamera()
        setupTouchpad()
        setupMicrophone()
        setupNavigationButtons()
    }
    override fun onStop() {
        super.onStop()
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // --- SETUP METHODS --- //
    @SuppressLint("DefaultLocale")
    private fun setupCalendar() {
        val dateInput = findViewById<EditText>(R.id.TaskDueDate)
        dateInput.setOnClickListener {
            val c = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(this, { _, y, m, d ->
                dateInput.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
            datePickerDialog.datePicker.firstDayOfWeek = Calendar.MONDAY
            datePickerDialog.show()
        }
    }
    private fun setupLocation() {
        val locationInput = findViewById<AutoCompleteTextView>(R.id.TaskLocation)
        locationAdapter = ArrayAdapter(this, R.layout.item_location_suggestion, android.R.id.text1, mutableListOf())
        locationInput.setAdapter(locationAdapter)

        locationInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        locationInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.length < 3) return
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    fetchPhotonData(query, locationInput)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                selectedLat = null
                selectedLon = null
            }
        })

        locationInput.setOnItemClickListener { parent, _, position, _ ->
            val selection = parent.getItemAtPosition(position) as String
            val coords = locationDataMap[selection]
            selectedLat = coords?.first
            selectedLon = coords?.second
        }
    }
    private fun setupCamera() {
        val photoButton = findViewById<Button>(R.id.PhotoButton)
        photoButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }
    private fun setupTouchpad() {
        findViewById<Button>(R.id.HandwrittenButton).setOnClickListener {
            showDrawingDialog()
        }
    }
    private fun setupMicrophone() {
        findViewById<Button>(R.id.microphoneButton).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                handleAudioAction()
            } else {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    private fun setupNavigationButtons() {
        val nameInput = findViewById<EditText>(R.id.TaskName)
        val descInput = findViewById<EditText>(R.id.TaskDescription)
        val dateInput = findViewById<EditText>(R.id.TaskDueDate)
        val locationInput = findViewById<AutoCompleteTextView>(R.id.TaskLocation)
        val seekBar = findViewById<SeekBar>(R.id.seekBar)

        findViewById<Button>(R.id.AddButton).setOnClickListener {
            val name = nameInput.text.toString().trim()
            val address = locationInput.text.toString().trim()
            val desc = descInput.text.toString()
            val date = dateInput.text.toString()

            if (name.isEmpty()) {
                Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                var finalLat = selectedLat
                var finalLon = selectedLon

                // --- WHAT IF ADDRESS WAS WRITTEN BUT NOT CHOSEN FROM THE PROPOSITIONS? --- //
                if (address.isNotEmpty() && (finalLat == null || finalLon == null)) {
                    val cached = locationDataMap[address]
                    if (cached != null) {
                        finalLat = cached.first
                        finalLon = cached.second
                    } else {
                        val coords = fetchCoordsDirectly(address)
                        if (coords != null) {
                            finalLat = coords.first
                            finalLon = coords.second
                        }
                    }
                }

                val waterPoints = if (seekBar.progress == 0) null else seekBar.progress

                saveTask(name = name, desc = desc, date = date, photo = savedPhotoPath, handwrtt = currentHandwritingPath,
                    addr = address, lat = finalLat, lon = finalLon, water = waterPoints, voice = currentVoicePath)
            }
        }

        findViewById<Button>(R.id.BackButton).setOnClickListener {
            finish()
        }
    }

    // --- LOGIC METHODS --- //
    private fun saveTask(name: String, desc: String, date: String, photo: String?, handwrtt: String?, addr: String, lat: Double?, lon: Double?, water: Int?, voice: String?) {
        val task = Task(name = name, description = desc, dueDate = date, photo = photo, handwrittenPhoto = handwrtt,
            address = addr, latitude = lat, longitude = lon, waterPoints = water, voiceNote = voice)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(this@AddingTaskActivity)
                db.taskDao().insertTask(task)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddingTaskActivity, "Saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddingTaskActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- LOCATION API --- //
    private suspend fun fetchPhotonData(query: String, view: AutoCompleteTextView) {
        withContext(Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val response = URL("https://photon.komoot.io/api/?q=$encodedQuery&limit=5").readText()
                val json = JSONObject(response)
                val features = json.getJSONArray("features")
                val results = mutableListOf<String>()

                locationDataMap.clear()
                for (i in 0 until features.length()) {
                    val obj = features.getJSONObject(i)
                    val prop = obj.getJSONObject("properties")
                    val geom = obj.getJSONObject("geometry").getJSONArray("coordinates")

                    val name = if (prop.has("name")) prop.getString("name") else ""
                    val street = if (prop.has("street")) prop.getString("street") else ""
                    val houseNum = if (prop.has("housenumber")) prop.getString("housenumber") else ""
                    val city = if (prop.has("city")) prop.getString("city") else ""

                    val addressParts = mutableListOf<String>()
                    if (name.isNotEmpty()) addressParts.add(name)
                    if (street.isNotEmpty()) addressParts.add(street)
                    if (houseNum.isNotEmpty()) addressParts.add(houseNum)
                    if (city.isNotEmpty()) addressParts.add(city)

                    val label = addressParts.joinToString(", ")

                    if (label.isNotEmpty() && !results.contains(label)) {
                        results.add(label)
                        locationDataMap[label] = Pair(geom.getDouble(1), geom.getDouble(0))
                    }
                }

                withContext(Dispatchers.Main) {
                    locationAdapter.clear()
                    locationAdapter.addAll(results)
                    locationAdapter.notifyDataSetChanged()
                    if (results.isNotEmpty() && view.hasFocus()) {
                        view.showDropDown()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private suspend fun fetchCoordsDirectly(address: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(address, "UTF-8")
                val response = URL("https://photon.komoot.io/api/?q=$encoded&limit=1").readText()
                val json = JSONObject(response)
                val features = json.getJSONArray("features")
                if (features.length() > 0) {
                    val geom = features.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
                    return@withContext Pair(geom.getDouble(1), geom.getDouble(0))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }
    }

    // --- CAMERA --- //
    private fun launchCamera() {
        createImageUri()?.let { uri ->
            currentPhotoUri = uri
            takePictureLauncher.launch(uri)
        }
    }
    private fun createImageUri(): Uri? {
        val file = File(cacheDir, "camera_images").apply { mkdirs() }
            .let { File(it, "MG_${System.currentTimeMillis()}.jpg") }
        return FileProvider.getUriForFile(this, "${packageName}.provider", file)
    }

    // --- TOUCHPAD --- //
    private fun showDrawingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_drawing, null)
        val drawingView = dialogView.findViewById<DrawingView>(R.id.dialogDrawingView)
        val btnClear = dialogView.findViewById<Button>(R.id.btnDialogClear)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)
        val btnUndo = dialogView.findViewById<Button>(R.id.btnDialogUndo)
        val btnRedo = dialogView.findViewById<Button>(R.id.btnDialogRedo)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnUndo.setOnClickListener { drawingView.undo() }
        btnRedo.setOnClickListener { drawingView.redo() }
        btnClear.setOnClickListener { drawingView.clear() }

        btnSave.setOnClickListener {
            val path = saveBitmapAsPng(drawingView.getBitmap())
            if (path != null) {
                currentHandwritingPath = path
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()

                val hwPreview = findViewById<ImageView>(R.id.HandwrittenPreview)
                val hwButton = findViewById<Button>(R.id.HandwrittenButton)

                hwPreview.visibility = View.VISIBLE
                hwPreview.setImageBitmap(drawingView.getBitmap())
                hwButton.visibility = View.GONE
            }
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun saveBitmapAsPng(bitmap: Bitmap): String? {
        val fileName = "MG_HANDWRITTEN_${System.currentTimeMillis()}.png"
        val file = File(filesDir, fileName)
        return try {
            val out = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    // --- MICROPHONE --- //
    private fun handleAudioAction() {
        val micButton = findViewById<Button>(R.id.microphoneButton)

        if (!hasRecorded) {
            if (!isRecording) {
                currentVoicePath = "${externalCacheDir?.absolutePath}/MG_VOICE_${System.currentTimeMillis()}.3gp"
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setOutputFile(currentVoicePath)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    try {
                        prepare()
                        start()
                        isRecording = true
                        micButton.text = "STOP RECORDING"
                        Toast.makeText(this@AddingTaskActivity, "Recording...", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                hasRecorded = true
                micButton.text="PLAY RECORDING"
                Toast.makeText(this, "Voice Note Saved!", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (currentVoicePath != null) {
                mediaPlayer = MediaPlayer().apply {
                    try {
                        setDataSource(currentVoicePath)
                        prepare()
                        start()
                        micButton.isEnabled = false
                        micButton.text = "PLAYING"
                        Toast.makeText(this@AddingTaskActivity, "Playing...", Toast.LENGTH_SHORT).show()

                        setOnCompletionListener {
                            release()
                            mediaPlayer = null
                            micButton.isEnabled = true
                            micButton.text = "PLAY RECORDING"
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}