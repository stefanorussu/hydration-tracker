package com.stefanorussu.hydrationtracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Inizializzazione del DataStore dedicato alle impostazioni di backup
private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_prefs")

class BackupPreferencesManager(private val context: Context) {

    companion object {
        private val LOCAL_BACKUP_KEY = booleanPreferencesKey("local_backup_auto")
        private val DRIVE_SYNC_KEY = booleanPreferencesKey("drive_sync_auto")
    }

    // Legge se il backup locale automatico è attivo (default: false)
    val isLocalBackupEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { it[LOCAL_BACKUP_KEY] ?: false }

    // Legge se la sincronizzazione Drive è attiva (default: false)
    val isDriveSyncEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { it[DRIVE_SYNC_KEY] ?: false }

    // Salva la preferenza per il backup locale
    suspend fun setLocalBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[LOCAL_BACKUP_KEY] = enabled }
    }

    // Salva la preferenza per Drive
    suspend fun setDriveSyncEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[DRIVE_SYNC_KEY] = enabled }
    }
}