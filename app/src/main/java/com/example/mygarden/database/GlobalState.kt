package com.example.mygarden.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "globals")
data class GlobalState(
    @PrimaryKey
    val id: Int = 1,
    var darkTheme: Boolean = false,
    var soundOn: Boolean = true,
    var waterPoint: Int = 0,
    var plantIndex: Int = 0,
    var plantProgress: Int = 0
)