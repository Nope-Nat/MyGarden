package com.example.mygarden.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addingtask)

        val nameInput = findViewById<EditText>(R.id.TaskName)
        val descInput = findViewById<EditText>(R.id.TaskDescription)
        val dateInput = findViewById<EditText>(R.id.TaskDueDate)

        // --- CALENDAR --- //
        dateInput.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                dateInput.setText(String.format("%04d-%02d-%02d", y, m + 1, d))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        // --- FINDING LOCATION --- //
        val locationInput = findViewById<AutoCompleteTextView>(R.id.TaskLocation)
        locationAdapter = ArrayAdapter(this, R.layout.item_location_suggestion, android.R.id.text1, mutableListOf())
        locationInput.setAdapter(locationAdapter)

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

        // --- CAMERA --- //
        val photoPreview = findViewById<ImageView>(R.id.PhotoPreview)
        val photoButton = findViewById<Button>(R.id.PhotoButton)
        val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                savedPhotoPath = currentPhotoUri.toString()
                photoPreview.visibility = View.VISIBLE
                photoPreview.setImageURI(currentPhotoUri)
                photoButton.visibility = View.GONE
            }
        }
        photoButton.setOnClickListener {
            createImageUri()?.let { uri ->
                currentPhotoUri = uri
                takePicture.launch(uri)
            }
        }

        // --- TOUCHPAD ---
        findViewById<Button>(R.id.HandwrittenButton).setOnClickListener {
            showDrawingDialog()
        }


        // --- NAVIGATION BUTTONS --- //
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

                // --- WHAT IF ADDRESS WAS WRITTEN BUT NOT CHOSEN FROM THE PROPOSITIONS? --- ///
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
                saveTask(name, desc, date, savedPhotoPath, currentHandwritingPath, address, finalLat, finalLon)
            }
        }
        findViewById<Button>(R.id.BackButton).setOnClickListener {
            finish()
        }
    }

    private fun saveTask(name: String, desc: String, date: String, photo: String?, handwrtt: String?, addr: String, lat: Double?, lon: Double?) {
        lifecycleScope.launch(Dispatchers.IO) {
            val task = Task(name = name, description = desc, dueDate = date, photo = photo, handwrittenPhoto = handwrtt,
                address = addr, latitude = lat, longitude = lon)

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(this@AddingTaskActivity)
                    db.taskDao().insertTask(task)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@AddingTaskActivity, "Saved!", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@AddingTaskActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // --- LOCATION --- //
    private var searchJob: Job? = null
    private val locationDataMap = mutableMapOf<String, Pair<Double, Double>>()
    private lateinit var locationAdapter: ArrayAdapter<String>

    private var selectedLat: Double? = null
    private var selectedLon: Double? = null
    private suspend fun fetchPhotonData(query: String, view: AutoCompleteTextView) {
        withContext(Dispatchers.IO) {
            try {
                val response = URL("https://photon.komoot.io/api/?q=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=5").readText()
                val json = JSONObject(response)
                val features = json.getJSONArray("features")
                val results = mutableListOf<String>()

                locationDataMap.clear()
                for (i in 0 until features.length()) {
                    val obj = features.getJSONObject(i)
                    val prop = obj.getJSONObject("properties")
                    val geom = obj.getJSONObject("geometry").getJSONArray("coordinates")

                    val name = if(prop.has("name")) prop.getString("name") else ""
                    val street = if(prop.has("street")) prop.getString("street") else ""
                    val houseNum = if(prop.has("housenumber")) prop.getString("housenumber") else ""
                    val city = if(prop.has("city")) prop.getString("city") else ""

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
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    private suspend fun fetchCoordsDirectly(address: String): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(address, "UTF-8")
                val response =
                    URL("https://photon.komoot.io/api/?q=$encoded&limit=1").readText()
                val json = JSONObject(response)
                val features = json.getJSONArray("features")
                if (features.length() > 0) {
                    val geom = features.getJSONObject(0).getJSONObject("geometry")
                        .getJSONArray("coordinates")
                    return@withContext Pair(geom.getDouble(1), geom.getDouble(0))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }
    }

    // --- PHOTOS --- //
    private var currentPhotoUri: Uri? = null
    private var savedPhotoPath: String? = null
    private fun createImageUri(): Uri? {
        val file = File(cacheDir, "camera_images").apply { mkdirs() }
            .let { File(it, "MG_${System.currentTimeMillis()}.jpg") }
        return FileProvider.getUriForFile(this, "${packageName}.provider", file)
    }

    // --- TOUCHPAD --- //
    private var currentHandwritingPath: String? = null
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
    private fun showDrawingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_drawing, null)
        val drawingView = dialogView.findViewById<DrawingView>(R.id.dialogDrawingView)
        val btnClear = dialogView.findViewById<Button>(R.id.btnDialogClear)
        val btnSave = dialogView.findViewById<Button>(R.id.btnDialogSave)
        val btnUndo = dialogView.findViewById<Button>(R.id.btnDialogUndo)
        val btnRedo = dialogView.findViewById<Button>(R.id.btnDialogRedo)

        btnUndo.setOnClickListener {
            drawingView.undo()
        }

        btnRedo.setOnClickListener {
            drawingView.redo()
        }

        val dialog = AlertDialog.Builder(this).setView(dialogView)
            .setCancelable(true).create()

        btnClear.setOnClickListener {
            drawingView.clear()
        }

        btnSave.setOnClickListener {
            val path = saveBitmapAsPng(drawingView.getBitmap())
            if (path != null) {
                currentHandwritingPath = path
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            }
            val hwPreview = findViewById<ImageView>(R.id.HandwrittenPreview)
            hwPreview.visibility = View.VISIBLE
            hwPreview.setImageBitmap(drawingView.getBitmap())
            val hwButton = findViewById<Button>(R.id.HandwrittenButton)
            hwButton.visibility = View.GONE
            dialog.dismiss()
        }
        dialog.show()
    }
}