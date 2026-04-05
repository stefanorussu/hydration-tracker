package com.stefanorussu.hydrationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stefanorussu.hydrationtracker.data.local.ThemePreferencesManager

class SettingsViewModelFactory(private val themeManager: ThemePreferencesManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(themeManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}