package com.stefanorussu.hydrationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stefanorussu.hydrationtracker.data.repository.FitbitRepository
import com.stefanorussu.hydrationtracker.data.repository.WaterRepository

class WaterViewModelFactory(
    private val waterRepository: WaterRepository,
    private val fitbitRepository: FitbitRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WaterViewModel(waterRepository, fitbitRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}