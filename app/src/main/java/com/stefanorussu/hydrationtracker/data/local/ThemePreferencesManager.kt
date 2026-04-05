package com.stefanorussu.hydrationtracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Estensione per inizializzare il DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

class ThemePreferencesManager(private val context: Context) {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    // Valori costanti per evitare errori di battitura
    object ThemeMode {
        const val SYSTEM = "SYSTEM"
        const val LIGHT = "LIGHT"
        const val DARK = "DARK"
    }

    // Legge il tema salvato (Default: SYSTEM)
    val themeModeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM
        }

    // Salva il tema scelto dall'utente
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }
}