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
        val AGE = intPreferencesKey("user_age")
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
            // Se ACTIVITY_LEVEL non è una stringa o è nullo, usiamo il default
            val activityName = try {
                preferences[Keys.ACTIVITY_LEVEL] ?: ActivityLevel.MODERATE.name
            } catch (e: ClassCastException) {
                // Se c'è un vecchio dato Float, restituiamo il default
                ActivityLevel.MODERATE.name
            }

            val activityLevel = try {
                ActivityLevel.valueOf(activityName)
            } catch (e: Exception) {
                ActivityLevel.MODERATE
            }

            UserProfile(
                weightKg = preferences[Keys.WEIGHT] ?: 70f,
                age = preferences[Keys.AGE] ?: 25,
                isMale = preferences[Keys.IS_MALE] ?: true,
                activityLevel = activityLevel
            )
        }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.userProfileStore.edit { preferences ->
            preferences[Keys.WEIGHT] = profile.weightKg
            preferences[Keys.AGE] = profile.age
            preferences[Keys.IS_MALE] = profile.isMale
            preferences[Keys.ACTIVITY_LEVEL] = profile.activityLevel.name
        }
    }
}