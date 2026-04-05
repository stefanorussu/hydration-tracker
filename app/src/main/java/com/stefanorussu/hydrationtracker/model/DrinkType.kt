package com.stefanorussu.hydrationtracker.model

/**
 * Rappresenta i tipi di bevande e la loro percentuale di idratazione.
 * Esempio: Il latte idrata all'87% perché il resto sono grassi/proteine.
 */
enum class DrinkType(val waterPercentage: Double, val icon: String) {
    WATER(1.0, "💧"),
    TEA(0.99, "🍵"),
    COFFEE(0.98, "☕"),
    SODA(0.89, "🥤"),
    MILK(0.87, "🥛"),
    JUICE(0.85, "🧃"),
    BEER(0.92, "🍺"),
    SMOOTHIE(0.80, "🥤"),
    YOGURT(0.75, "🍧");

    /**
     * Calcola quanta acqua effettiva viene assunta.
     * @param amountMl La quantità totale di liquido versato in ml.
     */
    fun calculateWaterAmount(amountMl: Int): Int {
        return (amountMl * waterPercentage).toInt()
    }
}