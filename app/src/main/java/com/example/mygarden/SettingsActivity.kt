package com.example.mygarden

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.switchmaterial.SwitchMaterial

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

        // --- Obsługa trybu ciemnego --- //
        val themeSwitch: SwitchMaterial = findViewById(R.id.DarkModeSwitch)
        val sharedPref = getSharedPreferences("Settings", MODE_PRIVATE)
        val editor = sharedPref.edit()

// Sprawdzamy zapisaną preferencję (domyślnie false - tryb jasny)
        val isDarkModeSaved = sharedPref.getBoolean("dark_mode_key", false)
        themeSwitch.isChecked = isDarkModeSaved

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            // Zapisujemy decyzję!
            editor.putBoolean("dark_mode_key", isChecked)
            editor.apply()
        }

        // --- Obsługa dźwięku --- //
        val soundSwitch: SwitchMaterial = findViewById(R.id.SoundSwitch)

        val isSoundEnabled = sharedPref.getBoolean("sound_key", true)
        soundSwitch.isChecked = isSoundEnabled
        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("sound_key", isChecked)
            editor.apply()
        }
    }
}