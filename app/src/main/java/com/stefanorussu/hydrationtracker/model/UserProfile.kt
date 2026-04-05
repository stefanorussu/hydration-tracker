package com.stefanorussu.hydrationtracker.model

data class UserProfile(
    val weightKg: Double,
    val age: Int,
    val isMale: Boolean
) {
    /**
     * Calcola l'obiettivo giornaliero basato sulla formula:
     * (Peso * 30ml) + correzioni per età.
     */
    fun calculateDailyGoalMl(): Int {
        var goal = (weightKg * 30).toInt()

        // Gli anziani hanno bisogno di meno, i giovani di più
        when {
            age < 30 -> goal += 200
            age > 55 -> goal -= 200
        }

        // Differenza statistica media tra i sessi
        if (isMale) goal += 300

        return goal
    }
}