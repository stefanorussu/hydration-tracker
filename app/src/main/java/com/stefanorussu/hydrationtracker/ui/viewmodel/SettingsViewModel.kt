package com.stefanorussu.hydrationtracker.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.stefanorussu.hydrationtracker.data.backup.BackupRepository
import com.stefanorussu.hydrationtracker.data.backup.GoogleDriveManager
import com.stefanorussu.hydrationtracker.data.local.BackupPreferencesManager
import com.stefanorussu.hydrationtracker.data.local.ThemePreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val themePreferencesManager: ThemePreferencesManager,
    private val backupRepository: BackupRepository,
    private val backupPrefsManager: BackupPreferencesManager,
    val driveManager: GoogleDriveManager // <-- Eccolo qui, il quarto argomento!
) : ViewModel() {

    // --- TEMA ---
    val themeMode = themePreferencesManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreferencesManager.ThemeMode.SYSTEM)

    fun setThemeMode(mode: String) = viewModelScope.launch { themePreferencesManager.setThemeMode(mode) }

    // --- PREFERENZE BACKUP (SWITCH) ---
    val isLocalBackupEnabled = backupPrefsManager.isLocalBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDriveSyncEnabled = backupPrefsManager.isDriveSyncEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleLocalBackup(enabled: Boolean) = viewModelScope.launch { backupPrefsManager.setLocalBackupEnabled(enabled) }
    fun toggleDriveSync(enabled: Boolean) = viewModelScope.launch { backupPrefsManager.setDriveSyncEnabled(enabled) }

    // --- MESSAGGI UI ---
    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage = _backupMessage.asStateFlow()
    fun clearBackupMessage() { _backupMessage.value = null }

    // --- AZIONI SAF (URI LOCALE ESTERNO) ---
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.exportToExternalUri(uri)
            _backupMessage.value = if (result.isSuccess) "Backup salvato correttamente!" else "Errore salvataggio"
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.importFromExternalUri(uri)
            if (result.isSuccess) {
                _backupMessage.value = "Ripristinati ${result.getOrNull()} record!"
            } else {
                _backupMessage.value = "Errore durante il ripristino"
            }
        }
    }

    // --- AZIONI GOOGLE DRIVE ---

    fun backupToGoogleDrive(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _backupMessage.value = "Caricamento su Drive in corso..."
            val result = backupRepository.backupToDrive(account)
            if (result.isSuccess) {
                _backupMessage.value = "Backup su Drive completato! ☁️"
            } else {
                _backupMessage.value = "Errore upload Drive: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun restoreFromGoogleDrive(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _backupMessage.value = "Scaricamento da Drive in corso..."
            val result = backupRepository.restoreFromDrive(account)
            if (result.isSuccess) {
                _backupMessage.value = "Ripristinati ${result.getOrNull()} record da Drive!"
            } else {
                _backupMessage.value = "Errore download Drive: ${result.exceptionOrNull()?.message}"
            }
        }
    }
}