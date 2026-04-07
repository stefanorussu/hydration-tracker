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

    // 1. Aggiunto il Context come parametro
    fun updateProfile(context: Context, newProfile: UserProfile) {
        viewModelScope.launch {
            // Salva il profilo nel DataStore
            profileManager.saveUserProfile(newProfile)

            // 2. Calcola l'obiettivo e lo salva nelle SharedPreferences per il Worker
            val nuovoObiettivo = calculateGoal(newProfile)
            val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)
            prefs.edit().putInt("daily_goal_ml", nuovoObiettivo).apply()
        }
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