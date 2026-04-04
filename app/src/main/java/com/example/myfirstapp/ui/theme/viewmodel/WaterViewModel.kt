package com.example.myfirstapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstapp.data.local.entities.WaterLog
import com.example.myfirstapp.data.repository.WaterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WaterViewModel(private val repository: WaterRepository) : ViewModel() {

    // Lista osservabile dei log
    val logs: StateFlow<List<WaterLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Totale calcolato di oggi
    val todayTotal: StateFlow<Int> = repository.getTodayTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addDrink(amount: Int, type: DrinkType) {
        viewModelScope.launch {
            repository.addDrink(amount, type)
        }
    }

    fun removeLog(log: WaterLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }
}