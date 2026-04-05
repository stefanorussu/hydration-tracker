package com.stefanorussu.hydrationtracker.data.repository

import com.stefanorussu.hydrationtracker.data.local.WaterDao
import com.stefanorussu.hydrationtracker.data.local.entities.WaterLog
import com.stefanorussu.hydrationtracker.model.DrinkType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class WaterRepository(private val waterDao: WaterDao) {

    // Recupera tutti i log per la lista
    val allLogs: Flow<List<WaterLog>> = waterDao.getAllLogs()

    // Calcola il totale bevuto oggi
    fun getTodayTotal(): Flow<Int?> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return waterDao.getTodayWaterSum(startOfDay)
    }

    // Aggiunge una bevanda calcolando l'acqua reale
    suspend fun addDrink(amountMl: Int, type: DrinkType) {
        val waterContent = (amountMl * type.waterPercentage).toInt()

        val log = WaterLog(
            amountMl = amountMl,
            waterContentMl = waterContent,
            drinkType = type.name,
            timestamp = System.currentTimeMillis()
        )
        waterDao.insertLog(log)
    }

    // Elimina un log
    suspend fun deleteLog(log: WaterLog) {
        waterDao.deleteLog(log)
    }
}