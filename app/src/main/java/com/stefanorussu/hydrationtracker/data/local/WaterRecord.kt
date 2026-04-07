package com.stefanorussu.hydrationtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_records")
data class WaterRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountMl: Int,
    val timestamp: Long,
    val drinkName: String,

    // --- CAMPI FITBIT ---
    val source: String = "LOCAL", // Di base, tutto ciò che inserisci a mano è LOCAL
    val externalId: String? = null // Vuoto di default, si riempirà con l'ID di Fitbit
)