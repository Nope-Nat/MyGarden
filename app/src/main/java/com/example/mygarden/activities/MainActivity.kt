package com.example.mygarden.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Toolbar and Drawer Layout --- //
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

        // --- Recycler --- //
        recyclerView = findViewById(R.id.tasksRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        taskAdapter = TaskAdapter(emptyList(), {task -> task.dueDate}) { selectedTask ->
            val intent = Intent(this, DisplayingTaskActivity::class.java)
            intent.putExtra("TASK_ID", selectedTask.id)
            startActivity(intent)
        }
        recyclerView.adapter = taskAdapter

        // --- Navigation Drawer Menu --- //
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
                R.id.nav_main -> {}
                R.id.nav_plant -> {
                    startActivity(Intent(this, PlantActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)
            val tasks = db.taskDao().getAllUndoneTasks()
            withContext(Dispatchers.Main) {
                taskAdapter.updateTasks(tasks)
            }
        }
    }
}