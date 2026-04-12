package com.example.mygarden.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {
    @Insert
    suspend fun insertTask(task: Task)

    @Query("""
        SELECT * FROM tasks 
        WHERE done = false 
        ORDER BY 
            CASE 
                WHEN dueDate IS NULL OR dueDate = '' THEN 1 
                ELSE 0 
        END ASC, dueDate ASC
    """)
    suspend fun getAllUndoneTasks(): List<Task>

    @Query("""
        SELECT * FROM tasks 
        WHERE done = true 
        ORDER BY doneDate ASC
    """)
    suspend fun getAllDoneTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :needed_id")
    suspend fun getTaskById(needed_id: Int): Task?

    @Update
    suspend fun updateTask(task: Task)

    @Query ("DELETE FROM tasks")
    suspend fun clearTasks()
}