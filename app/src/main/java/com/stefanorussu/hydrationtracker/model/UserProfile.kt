package com.stefanorussu.hydrationtracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// ECCO LA CORREZIONE: Ora ogni livello ha il suo "moltiplicatore" (ml per kg)
enum class ActivityLevel(val multiplier: Int) {
    LOW(30),       // Sedentario: 30 ml per ogni kg di peso
    MODERATE(35),  // Attivo: 35 ml per ogni kg di peso
    HIGH(40)       // Molto Attivo: 40 ml per ogni kg di peso
}

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val weightKg: Float,
    val birthDate: String,
    val isMale: Boolean,
    val activityLevel: ActivityLevel
)