package com.stefanorussu.hydrationtracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Estensione privata per il DataStore
private val Context.userProfileStore: DataStore<Preferences> by preferencesDataStore(name = "user_profile_prefs")

class UserProfileManager(private val context: Context) {

    // CHIAVI UNICHE PER IL PROFILO
    private object Keys {
        val WEIGHT = floatPreferencesKey("user_weight")
        val BIRTH_DATE = stringPreferencesKey("user_birth_date") // <--- Ora si aspetta una data!
        val IS_MALE = booleanPreferencesKey("user_is_male")
        val ACTIVITY_LEVEL = stringPreferencesKey("user_activity_level")
    }

    val userProfileFlow: Flow<UserProfile> = context.userProfileStore.data
        .catch { exception ->
            // Gestisce errori di lettura del file DataStore
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // PREVENZIONE CRASH: Leggiamo in modo sicuro
            val activityName = try {
                preferences[Keys.ACTIVITY_LEVEL] ?: ActivityLevel.MODERATE.name
            } catch (e: ClassCastException) {
                ActivityLevel.MODERATE.name
            }

            val activityLevel = try {
                ActivityLevel.valueOf(activityName)
            } catch (e: Exception) {
                ActivityLevel.MODERATE
            }

            UserProfile(
                weightKg = preferences[Keys.WEIGHT] ?: 70f,
                birthDate = preferences[Keys.BIRTH_DATE] ?: "2000-01-01", // <--- Lettura aggiornata
                isMale = preferences[Keys.IS_MALE] ?: true,
                activityLevel = activityLevel
            )
        }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.userProfileStore.edit { preferences ->
            preferences[Keys.WEIGHT] = profile.weightKg
            preferences[Keys.BIRTH_DATE] = profile.birthDate // <--- Salvataggio aggiornato
            preferences[Keys.IS_MALE] = profile.isMale
            preferences[Keys.ACTIVITY_LEVEL] = profile.activityLevel.name
        }
    }
}