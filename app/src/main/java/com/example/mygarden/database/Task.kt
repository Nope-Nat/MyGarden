package com.example.mygarden.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String?,
    val dueDate: String?,
    var doneDate: String? = null,
    var done: Boolean = false,
    val photo: String? = null,
    val handwrittenPhoto: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)