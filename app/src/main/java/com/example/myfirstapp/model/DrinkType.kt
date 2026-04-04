package com.stefanorussu.hydrationtracker.model

enum class DrinkType(val waterPercentage: Float) {
    WATER(1.0f),
    MILK(0.87f),
    SODA(0.90f),
    COFFEE(0.99f),
    YOGURT(0.80f)
}