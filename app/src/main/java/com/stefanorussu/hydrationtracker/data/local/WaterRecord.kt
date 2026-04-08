package com.stefanorussu.hydrationtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_logs")
data class WaterRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountMl: Int,
    val inputAmountMl: Int = amountMl, // <--- NUOVO: Salva il liquido fisico reale! (Di base è uguale all'acqua)
    val timestamp: Long,
    val drinkName: String,
    val source: String = "LOCAL",
    val externalId: String? = null
)