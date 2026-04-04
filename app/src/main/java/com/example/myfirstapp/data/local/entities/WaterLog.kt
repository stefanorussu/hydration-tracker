package com.example.myfirstapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.stefanorussu.hydrationtracker.model.DrinkType

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMl: Int,
    val waterContentMl: Int,
    val drinkType: DrinkType,
    val timestamp: Long = System.currentTimeMillis(),
    val fitbitSynced: Boolean = false
)