package com.example.mygarden.activities

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.mygarden.database.AppDatabase
import com.example.mygarden.database.GlobalState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyGarden : Application() {

    private var applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        val db = AppDatabase.getDatabase(this)
        applicationScope.launch {
            var state = db.globalStateDao().getState(1)
            if (state == null) {
                val newState = GlobalState(darkTheme = false, soundOn = true, waterPoint = 0)
                db.globalStateDao().insertState(newState)
                state = newState
            }
            applyTheme(state.darkTheme)
        }
    }

    private fun applyTheme(darkTheme: Boolean) {
        if (darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}