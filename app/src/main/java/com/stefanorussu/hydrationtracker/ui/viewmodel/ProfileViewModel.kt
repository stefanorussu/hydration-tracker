package com.stefanorussu.hydrationtracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefanorussu.hydrationtracker.data.local.UserProfile
import com.stefanorussu.hydrationtracker.data.local.UserProfileManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val profileManager: UserProfileManager) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = profileManager.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    fun updateProfile(context: Context, newProfile: UserProfile) {
        viewModelScope.launch {
            profileManager.saveUserProfile(newProfile)

            val nuovoObiettivo = calculateGoal(newProfile)
            val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("daily_goal_ml", nuovoObiettivo).apply()
        }
    }

    // --- FUNZIONE INTELLIGENTE PER FITBIT ---
    fun updateWeightFromFitbit(context: Context, newWeight: Float): Boolean {
        val currentProfile = userProfile.value
        // Aggiorna solo se il peso su Fitbit è diverso dal nostro salvato
        if (currentProfile.weightKg != newWeight) {
            val updatedProfile = currentProfile.copy(weightKg = newWeight)
            updateProfile(context, updatedProfile)
            return true // Indica che l'obiettivo è cambiato
        }
        return false // Il peso era uguale, nessuna modifica
    }

    fun calculateGoal(profile: UserProfile): Int {
        val baseGoal = profile.weightKg * profile.activityLevel.multiplier

        val genderBonus = if (profile.isMale) 200 else 0

        val ageAdjustment = when {
            profile.age < 30 -> 100
            profile.age > 60 -> -100
            else -> 0
        }

        return (baseGoal + genderBonus + ageAdjustment).toInt()
    }
}