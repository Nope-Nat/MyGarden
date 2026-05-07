package com.example.mygarden.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import com.example.mygarden.database.GlobalState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsActivity : BaseActivity() {
    private lateinit var drawerLayout: DrawerLayout

    // --- LIFECYCLE --- //
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupDrawerAndToolbar()
        setupDarkMode()
        setupSound()
        setupButtons()
    }

    // --- SETUP METHODS --- //
    private fun setupDrawerAndToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_add_task -> {
                    startActivity(Intent(this, AddingTaskActivity::class.java))
                }
                R.id.nav_settings -> {}
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                }
                R.id.nav_main -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                R.id.nav_plant -> {
                    startActivity(Intent(this, PlantActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }
    private fun setupDarkMode() {
        val themeSwitch: SwitchMaterial = findViewById(R.id.darkThemeSwitch)
        val dao = AppDatabase.getDatabase(this).globalStateDao()

        lifecycleScope.launch {
            val state = withContext(Dispatchers.IO) { dao.getState(1) }
            val isDarkMode = state.darkTheme

            themeSwitch.setOnCheckedChangeListener(null)
            themeSwitch.isChecked = isDarkMode

            themeSwitch.setOnCheckedChangeListener { _, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val freshState = dao.getState(1)
                    val updatedState = freshState.copy(darkTheme = isChecked)
                    dao.updateState(updatedState)
                }

                lifecycleScope.launch(Dispatchers.Main) {
                    kotlinx.coroutines.delay(150)
                    if (isChecked) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
            }
        }
    }

    private fun setupSound() {
        val soundSwitch: SwitchMaterial = findViewById(R.id.soundSwitch)
        val dao = AppDatabase.getDatabase(this).globalStateDao()

        lifecycleScope.launch {
            val state = withContext(Dispatchers.IO) { dao.getState(1) }
            val isSoundEnabled = state.soundOn

            soundSwitch.setOnCheckedChangeListener(null)
            soundSwitch.isChecked = isSoundEnabled

            soundSwitch.setOnCheckedChangeListener { _, isChecked ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val freshState = dao.getState(1)
                    val updatedState = freshState.copy(soundOn = isChecked)
                    dao.updateState(updatedState)
                }
            }
        }
    }
    private fun setupButtons() {
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
            db.globalStateDao().insertState(GlobalState(darkTheme = false, soundOn = true, waterPoint = 0, plantIndex = 0, plantProgress = 0))
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