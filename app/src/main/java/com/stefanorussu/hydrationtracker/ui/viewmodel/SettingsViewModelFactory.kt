package com.stefanorussu.hydrationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stefanorussu.hydrationtracker.data.backup.BackupRepository
import com.stefanorussu.hydrationtracker.data.backup.GoogleDriveManager
import com.stefanorussu.hydrationtracker.data.local.BackupPreferencesManager
import com.stefanorussu.hydrationtracker.data.local.ThemePreferencesManager

class SettingsViewModelFactory(
    private val themePreferencesManager: ThemePreferencesManager,
    private val backupRepository: BackupRepository,
    private val backupPrefsManager: BackupPreferencesManager,
    private val driveManager: GoogleDriveManager // <-- Aggiunto il gestore di Drive
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Ora passiamo 4 strumenti al ViewModel
            return SettingsViewModel(
                themePreferencesManager,
                backupRepository,
                backupPrefsManager,
                driveManager
            ) as T
        }
        throw IllegalArgumentException("Classe ViewModel sconosciuta")
    }
}