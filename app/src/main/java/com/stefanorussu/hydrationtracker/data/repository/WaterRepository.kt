package com.stefanorussu.hydrationtracker.data.repository

import com.stefanorussu.hydrationtracker.data.local.DailyWaterStats
import com.stefanorussu.hydrationtracker.data.local.DrinkFrequency
import com.stefanorussu.hydrationtracker.data.local.WaterDao
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import kotlinx.coroutines.flow.Flow

class WaterRepository(private val waterDao: WaterDao) {

    suspend fun insert(record: WaterRecord): Long {
        return waterDao.insert(record)
    }

    suspend fun updateFitbitId(recordId: Int, fitbitId: String) {
        waterDao.updateFitbitId(recordId, fitbitId)
    }

    suspend fun updateWater(record: WaterRecord) {
        waterDao.updateWater(record)
    }

    suspend fun deleteWater(record: WaterRecord) {
        waterDao.deleteWater(record)
    }

    fun getTodayTotal(startOfDay: Long, endOfDay: Long): Flow<Int?> {
        return waterDao.getTodayTotal(startOfDay, endOfDay)
    }

    fun getRecordsBetweenDates(startOfDay: Long, endOfDay: Long): Flow<List<WaterRecord>> {
        return waterDao.getRecordsBetweenDates(startOfDay, endOfDay)
    }

    fun getDrinkFrequencies(): Flow<List<DrinkFrequency>> {
        return waterDao.getDrinkFrequencies()
    }

    // RIPRISTINATO
    suspend fun getAllRecords(): List<WaterRecord> {
        return waterDao.getAllRecords()
    }

    fun getStatsBetweenDates(startOfDay: Long, endOfDay: Long): Flow<List<DailyWaterStats>> {
        return waterDao.getStatsBetweenDates(startOfDay, endOfDay)
    }

    suspend fun getMostFrequentAmount(drinkName: String): Int? {
        return waterDao.getMostFrequentAmount(drinkName)
    }
}