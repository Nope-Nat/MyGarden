package com.example.mygarden.activities

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MyGarden : Application() {
    override fun onCreate() {
        super.onCreate()

        val sharedPref = getSharedPreferences("Settings", MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean("dark_mode_key", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}