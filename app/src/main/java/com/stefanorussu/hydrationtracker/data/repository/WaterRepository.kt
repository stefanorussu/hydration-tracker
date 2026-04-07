package com.stefanorussu.hydrationtracker.data.repository

import com.stefanorussu.hydrationtracker.data.local.DailyWaterStats
import com.stefanorussu.hydrationtracker.data.local.DrinkFrequency
import com.stefanorussu.hydrationtracker.data.local.WaterDao
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class WaterRepository(private val waterDao: WaterDao) {

    // Manteniamo la versione con i parametri per la UI principale
    fun getTodayTotal(start: Long, end: Long): Flow<Int?> = waterDao.getTodayTotal(start, end)

    // Versione automatica SENZA parametri: calcola il tempo da sola
    // Questo risolve istantaneamente l'errore nel file HydrationReminderWorker
    fun getTodayTotal(): Flow<Int?> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        return waterDao.getTodayTotal(start, end)
    }

    fun getDrinkFrequencies(): Flow<List<DrinkFrequency>> = waterDao.getDrinkFrequencies()

    suspend fun insertWater(record: WaterRecord) {
        waterDao.insert(record)
    }

    suspend fun updateWater(record: WaterRecord) {
        waterDao.update(record)
    }

    suspend fun deleteTodayRecords() {
        waterDao.deleteToday()
    }

    suspend fun deleteWater(record: WaterRecord) {
        waterDao.delete(record)
    }

    fun getStatsBetweenDates(startTimestamp: Long, endTimestamp: Long, timezoneOffset: Long): Flow<List<DailyWaterStats>> {
        return waterDao.getStatsBetweenDates(startTimestamp, endTimestamp, timezoneOffset)
    }

    fun getRecordsBetweenDates(startTimestamp: Long, endTimestamp: Long): Flow<List<WaterRecord>> {
        return waterDao.getRecordsBetweenDates(startTimestamp, endTimestamp)
    }
}