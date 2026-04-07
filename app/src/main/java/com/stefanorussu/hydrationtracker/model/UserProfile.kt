package com.stefanorussu.hydrationtracker.data.local

enum class ActivityLevel(val multiplier: Float, val displayName: String) {
    SEDENTARY(30f, "Sedentario"),
    MODERATE(35f, "Moderato"),
    ACTIVE(40f, "Attivo"),
    ATHLETE(45f, "Atleta")
}

data class UserProfile(
    val weightKg: Float = 70f,
    val age: Int = 25,
    val isMale: Boolean = true,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE
)