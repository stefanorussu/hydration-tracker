package com.stefanorussu.hydrationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefanorussu.hydrationtracker.data.local.DrinkFrequency
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import com.stefanorussu.hydrationtracker.data.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar

class WaterViewModel(private val repository: WaterRepository) : ViewModel() {

    // Calcoliamo i limiti esatti del giorno corrente nel TUO fuso orario
    private val startOfDay: Long
    private val endOfDay: Long

    init {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        endOfDay = calendar.timeInMillis
    }

    val todayTotal: Flow<Int?> = repository.getTodayTotal(startOfDay, endOfDay)
    val dailyRecords: Flow<List<WaterRecord>> = repository.getRecordsBetweenDates(startOfDay, endOfDay)

    val drinkFrequencies: Flow<List<DrinkFrequency>> = repository.getDrinkFrequencies()

    fun addWater(amountMl: Int, drinkName: String) {
        viewModelScope.launch {
            val record = WaterRecord(
                amountMl = amountMl,
                timestamp = System.currentTimeMillis(),
                drinkName = drinkName
            )
            repository.insertWater(record)
        }
    }

    fun updateRecord(record: WaterRecord, newAmount: Int, newTimestamp: Long) {
        viewModelScope.launch {
            repository.updateWater(record.copy(amountMl = newAmount, timestamp = newTimestamp))
        }
    }

    fun deleteWater(record: WaterRecord) {
        viewModelScope.launch {
            repository.deleteWater(record)
        }
    }
}