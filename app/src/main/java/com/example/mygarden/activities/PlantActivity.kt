package com.example.mygarden.activities

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.example.mygarden.R
import com.google.android.material.navigation.NavigationView

class PlantActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant)

        setupDrawerAndToolbar()
        setupPlantLogic()
    }

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

        // --- Nawigation Drawer Menu --- //
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_add_task -> {
                    startActivity(Intent(this, AddingTaskActivity::class.java))
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.nav_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                }
                R.id.nav_main -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                R.id.nav_plant -> {}
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupPlantLogic() {
        val waterButton = findViewById<Button>(R.id.waterButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        waterButton.setOnClickListener {
            val currentProgress = progressBar.progress

            val newProgress = if (currentProgress >= 100) 0 else currentProgress + 10
            ObjectAnimator.ofInt(progressBar, "progress", newProgress).apply {
                duration = 300
                start()
            }
        }
    }
}