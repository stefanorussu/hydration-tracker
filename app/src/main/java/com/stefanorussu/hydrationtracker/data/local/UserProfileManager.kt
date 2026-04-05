package com.stefanorussu.hydrationtracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Estensione per inizializzare il DataStore del profilo
private val Context.userProfileStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile_prefs")

data class UserProfile(
    val weightKg: Float = 70f,
    val age: Int = 25,
    val isMale: Boolean = true,
    val activityLevel: Float = 1.2f // Moltiplicatore attività
)

class UserProfileManager(private val context: Context) {

    private object Keys {
        val WEIGHT = floatPreferencesKey("user_weight")
        val AGE = intPreferencesKey("user_age")
        val IS_MALE = booleanPreferencesKey("user_is_male")
        val ACTIVITY_LEVEL = floatPreferencesKey("user_activity_level")
    }

    // Esponiamo il profilo come Flow osservabile
    val userProfileFlow: Flow<UserProfile> = context.userProfileStore.data
        .map { preferences ->
            UserProfile(
                weightKg = preferences[Keys.WEIGHT] ?: 70f,
                age = preferences[Keys.AGE] ?: 25,
                isMale = preferences[Keys.IS_MALE] ?: true,
                activityLevel = preferences[Keys.ACTIVITY_LEVEL] ?: 1.2f
            )
        }

    // Funzione per salvare l'intero profilo
    suspend fun saveUserProfile(profile: UserProfile) {
        context.userProfileStore.edit { preferences ->
            preferences[Keys.WEIGHT] = profile.weightKg
            preferences[Keys.AGE] = profile.age
            preferences[Keys.IS_MALE] = profile.isMale
            preferences[Keys.ACTIVITY_LEVEL] = profile.activityLevel
        }
    }
}