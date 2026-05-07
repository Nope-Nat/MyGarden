package com.example.mygarden.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.mygarden.plants.PlantDataManager

class PlantActivity : BaseActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private val db by lazy { AppDatabase.getDatabase(this) }
    private lateinit var plantManager: PlantDataManager
    private var isAnimating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant)

        plantManager = PlantDataManager(this)

        setupDrawerAndToolbar()
        setupPlantLogic()
        observeGlobalState()
    }

    private fun observeGlobalState() {
        val pointsTextView = findViewById<TextView>(R.id.pointsCounter)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        db.globalStateDao().getStateFlow().asLiveData().observe(this) { state ->
            if (state != null) {
                pointsTextView.text = state.waterPoint.toString()
                plantManager.setPointer(state.plantIndex)
                updatePlantUI()
                if (!isAnimating) {
                    progressBar.progress = state.plantProgress
                }
            }
        }
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

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_add_task -> startActivity(Intent(this, AddingTaskActivity::class.java))
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_history -> startActivity(Intent(this, HistoryActivity::class.java))
                R.id.nav_main -> startActivity(Intent(this, MainActivity::class.java))
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
            if (isAnimating) return@setOnClickListener

            lifecycleScope.launch {
                val dao = db.globalStateDao()
                val state = withContext(Dispatchers.IO) { dao.getState(1) }

                if (state != null && state.waterPoint > 0) {
                    isAnimating = true

                    val currentProgress = state.plantProgress
                    val currentIndex = state.plantIndex

                    val newProgress = currentProgress + 1
                    if (newProgress >= 3) {
                        ObjectAnimator.ofInt(progressBar, "progress", 3).apply {
                            duration = 150
                            addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    lifecycleScope.launch(Dispatchers.IO) {

                                        val nextIndex =
                                            if (currentIndex >= plantManager.getTotalPlantsCount() - 1) 0 else currentIndex + 1
                                        val updatedState = state.copy(waterPoint = state.waterPoint - 1, plantProgress = 0, plantIndex = nextIndex)
                                        dao.updateState(updatedState)

                                        withContext(Dispatchers.Main) {
                                            if (nextIndex == 0) {
                                                Toast.makeText(this@PlantActivity, "You've grown all plants! Starting over!", Toast.LENGTH_SHORT).show()
                                            }
                                            isAnimating = false
                                        }
                                    }
                                }
                            })
                            start()
                        }
                    } else {
                        ObjectAnimator.ofInt(progressBar, "progress", newProgress).apply {
                            duration = 150
                            addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        val updatedState = state.copy(waterPoint = state.waterPoint - 1, plantProgress = newProgress)
                                        dao.updateState(updatedState)
                                        withContext(Dispatchers.Main) { isAnimating = false }
                                    }
                                }
                            })
                            start()
                        }
                    }
                } else {
                    Toast.makeText(this@PlantActivity, "No water drops, get some more!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updatePlantUI() {
        val plantImageView = findViewById<ImageView>(R.id.plantImage)
        val plantNameTextView = findViewById<TextView>(R.id.plantName)

        val currentPlant = plantManager.getCurrentPlant()

        if (currentPlant != null) {
            val bitmap = plantManager.getCurrentPlantBitmap()

            if (bitmap != null) {
                plantImageView.setImageBitmap(bitmap)
            } else {
                Toast.makeText(this, "Error finding file: ${currentPlant.fileName}", Toast.LENGTH_SHORT).show()
            }

            val plantName = currentPlant.category.replaceFirstChar { it.uppercase() }
            plantNameTextView.text = "${plantName.uppercase()} - Level ${currentPlant.level}"
        }
    }
}