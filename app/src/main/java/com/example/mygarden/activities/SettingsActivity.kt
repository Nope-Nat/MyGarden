package com.example.mygarden.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsActivity : BaseActivity() {

    // --- LIFECYCLE --- //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        val sharedPref = getSharedPreferences("Settings", MODE_PRIVATE)

        setupWindowInsets()
        setupDarkMode(sharedPref)
        setupSound(sharedPref)
        setupButtons()
    }

    // --- SETUP METHODS --- //

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupDarkMode(sharedPref: SharedPreferences) {
        val themeSwitch: SwitchMaterial = findViewById(R.id.DarkModeSwitch)
        val editor = sharedPref.edit()

        val isDarkModeSaved = sharedPref.getBoolean("dark_mode_key", false)
        themeSwitch.isChecked = isDarkModeSaved

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("dark_mode_key", isChecked).apply()

            val darkMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }

            if (AppCompatDelegate.getDefaultNightMode() != darkMode) {
                AppCompatDelegate.setDefaultNightMode(darkMode)
                val intent = intent
                finish()
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }

    private fun setupSound(sharedPref: SharedPreferences) {
        val soundSwitch: SwitchMaterial = findViewById(R.id.SoundSwitch)
        val editor = sharedPref.edit()

        val isSoundEnabled = sharedPref.getBoolean("sound_key", true)
        soundSwitch.isChecked = isSoundEnabled

        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("sound_key", isChecked).apply()
        }
    }

    private fun setupButtons() {
        // --- Back Button --- //
        findViewById<Button>(R.id.BackButton).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // --- Clearing Database --- //
        findViewById<Button>(R.id.WipeDataButton).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to wipe all data? This cannot be undone.")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Yes, Wipe") { _, _ ->
                    clearAllData()
                }
                .show()
        }
    }

    // --- LOGIC METHODS --- //

    private fun clearAllData() {
        lifecycleScope.launch(Dispatchers.IO) {
            clearFiles()
            val db = AppDatabase.getDatabase(this@SettingsActivity)
            db.taskDao().clearTasks()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SettingsActivity, "Data wiped", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearFiles() {
        try {
            val cameraImagesDir = File(cacheDir, "camera_images")
            if (cameraImagesDir.exists()) {
                cameraImagesDir.deleteRecursively()
            }
            filesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("MG_HANDWRITTEN_")) {
                    file.delete()
                }
            }
            externalCacheDir?.listFiles()?.forEach { file ->
                if (file.name.startsWith("MG_VOICE_")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}