package com.example.mygarden.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GlobalStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: GlobalState)

    @Update
    suspend fun updateState(state: GlobalState)

    @Query("SELECT * FROM globals WHERE id = :needed_id")
    suspend fun getState(needed_id: Int): GlobalState

    @Query("SELECT * FROM globals WHERE id = 1")
    fun getStateFlow(): Flow<GlobalState?>
}