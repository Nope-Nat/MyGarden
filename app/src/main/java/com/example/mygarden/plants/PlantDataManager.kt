package com.example.mygarden.plants

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class PlantDataManager(private val context: Context) {

    private val plantsList = mutableListOf<PlantData>()
    private var currentIndex = 0

    init {
        loadDataFromAssets()
    }
    private fun loadDataFromAssets() {
        try {
            context.assets.open("plants/a.out").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.trim().split(",")

                    if (parts.size >= 4) {
                        val id = parts[0].trim().toIntOrNull()
                        if (id != null) {
                            plantsList.add(PlantData(id = id, fileName = parts[1].trim(), category = parts[2].trim(), level = parts[3].trim().toIntOrNull() ?: 1))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- POINTER LOGIC --- //
    fun getCurrentPlant(): PlantData? {
        return if (plantsList.isNotEmpty()) plantsList[currentIndex] else null
    }
    fun setPointer(index: Int) {
        if (index in 0 until plantsList.size) {
            currentIndex = index
        }
    }

    // --- FINDING BITMAP --- //
    fun getCurrentPlantBitmap(): Bitmap? {
        val currentPlant = getCurrentPlant() ?: return null
        val assetPath = "plants/${currentPlant.category}/${currentPlant.fileName}"

        return try {
            val inputStream = context.assets.open(assetPath)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            println("Cannot open: '$assetPath'")
            null
        }
    }
    fun getTotalPlantsCount(): Int {
        return plantsList.size
    }
}