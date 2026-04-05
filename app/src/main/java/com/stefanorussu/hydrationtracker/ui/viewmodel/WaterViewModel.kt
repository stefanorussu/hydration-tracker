package com.stefanorussu.hydrationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefanorussu.hydrationtracker.data.local.entities.WaterLog
import com.stefanorussu.hydrationtracker.data.repository.WaterRepository
import com.stefanorussu.hydrationtracker.model.DrinkType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WaterViewModel(private val repository: WaterRepository) : ViewModel() {

    // Lista osservabile dei log
    val logs: StateFlow<List<WaterLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Usiamo .map { it ?: 0 } per convertire il null in 0 prima di passarlo allo StateFlow
    val todayTotal: StateFlow<Int> = repository.getTodayTotal()
        .map { it ?: 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun addDrink(amount: Int, type: DrinkType) {
        viewModelScope.launch {
            repository.addDrink(amount, type)
        }
    }

    fun removeLog(log: WaterLog) {
        viewModelScope.launch {
            repository.deleteLog(log) // Deve corrispondere al nome nel Repository
        }
    }
}