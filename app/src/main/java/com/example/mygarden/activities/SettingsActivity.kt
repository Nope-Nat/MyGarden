package com.example.mygarden.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mygarden.R
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
        val button = findViewById<Button>(R.id.BackButton)
        button.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}