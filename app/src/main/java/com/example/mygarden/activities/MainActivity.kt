package com.example.mygarden.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RadioGroup
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.mygarden.R
import com.example.mygarden.database.AppDatabase
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView

    private var currentSortAttribute: String? = null
    private var currentSortOrder: String? = null

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

        // --- Adding Task Shortcut --- //
        findViewById<FloatingActionButton>(R.id.fastAddButton).setOnClickListener {
            startActivity(Intent(this, AddingTaskActivity::class.java))
        }
        // --- Sorting --- //
        findViewById<FloatingActionButton>(R.id.sortButton).setOnClickListener {
            showSortDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun showSortDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sort, null)
        val rgAttribute = dialogView.findViewById<RadioGroup>(R.id.rgSortAttribute)
        val rgOrder = dialogView.findViewById<RadioGroup>(R.id.rgSortOrder)

        MaterialAlertDialogBuilder(this)
            .setTitle("Sorting")
            .setView(dialogView)
            .setPositiveButton("Sort") { dialog, _ ->
                val selectedAttributeId = rgAttribute.checkedRadioButtonId
                currentSortAttribute = when (selectedAttributeId) {
                    R.id.rbPoints -> "POINTS"
                    R.id.rbName -> "NAME"
                    R.id.rbDate -> "DATE"
                    R.id.rbProgress -> "PROGRESS"
                    else -> "POINTS"
                }

                val selectedOrderId = rgOrder.checkedRadioButtonId
                currentSortOrder = when (selectedOrderId) {
                    R.id.rbAsc -> "ASC"
                    R.id.rbDesc -> "DESC"
                    else -> "ASC"
                }

                loadTasks()
                dialog.dismiss()
            }
            .setNegativeButton("Back") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun loadTasks() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@MainActivity)

            val tasks = if (currentSortAttribute != null && currentSortOrder != null) {
                val columnName = when (currentSortAttribute) {
                    "NAME" -> "name"
                    "DATE" -> "dueDate"
                    "PROGRESS" -> "progress"
                    else -> "waterPoints"
                }
                val sortOrder = if (currentSortOrder == "DESC") "DESC" else "ASC"

                val queryString = if (currentSortAttribute == "DATE") {
                    "SELECT * FROM tasks WHERE done = 0 ORDER BY CASE WHEN dueDate IS NULL OR dueDate = '' THEN 1 ELSE 0 END $sortOrder, dueDate $sortOrder, name ASC"
                } else {
                    "SELECT * FROM tasks WHERE done = 0 ORDER BY $columnName $sortOrder"
                }

                val query = SimpleSQLiteQuery(queryString)
                db.taskDao().getTasksDynamically(query)
            } else {
                db.taskDao().getAllUndoneTasks()
            }

            withContext(Dispatchers.Main) {
                taskAdapter.updateTasks(tasks)
            }
        }
    }
}