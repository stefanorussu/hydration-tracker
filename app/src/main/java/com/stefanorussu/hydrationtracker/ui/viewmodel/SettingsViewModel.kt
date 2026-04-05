package com.stefanorussu.hydrationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefanorussu.hydrationtracker.data.local.ThemePreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val preferencesManager: ThemePreferencesManager) : ViewModel() {

    // Espone lo stato del tema come StateFlow (osservabile dalla UI)
    // Usiamo stateIn per convertire il Flow del DataStore in uno Stato persistente nel ViewModel
    val themeMode: StateFlow<String> = preferencesManager.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePreferencesManager.ThemeMode.SYSTEM
        )

    // Funzione per aggiornare il tema nel DataStore
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }
}