package com.example.mygarden.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Dark Mode Support --- //
        val themeSwitch: SwitchMaterial = findViewById(R.id.DarkModeSwitch)
        val sharedPref = getSharedPreferences("Settings", MODE_PRIVATE)
        val editor = sharedPref.edit()
        val isDarkModeSaved = sharedPref.getBoolean("dark_mode_key", false)
        themeSwitch.isChecked = isDarkModeSaved

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            editor.putBoolean("dark_mode_key", isChecked)
            editor.apply()
        }

        // --- Disabling/Enabling Sound --- //
        val soundSwitch: SwitchMaterial = findViewById(R.id.SoundSwitch)
        val isSoundEnabled = sharedPref.getBoolean("sound_key", true)
        soundSwitch.isChecked = isSoundEnabled


        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("sound_key", isChecked)
            editor.apply()
        }

        // --- Back Button --- //
        val button = findViewById<Button>(R.id.BackButton)
        button.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // --- Clearing Database --- //
        val wipeDataButton = findViewById<Button>(R.id.WipeDataButton)
        wipeDataButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to wipe all data? This cannot be undone.")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Yes, Wipe") { _, _ ->
                    clearDB()
                }
                .show()
        }
    }
    private fun clearDB() {
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@SettingsActivity).taskDao().clearTasks()

            launch(Dispatchers.Main) {
                Toast.makeText(this@SettingsActivity, "Data wiped", Toast.LENGTH_SHORT).show()
            }
        }
    }
}