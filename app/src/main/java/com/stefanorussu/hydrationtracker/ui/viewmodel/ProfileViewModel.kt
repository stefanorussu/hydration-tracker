package com.stefanorussu.hydrationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefanorussu.hydrationtracker.data.local.UserProfile
import com.stefanorussu.hydrationtracker.data.local.UserProfileManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val profileManager: UserProfileManager) : ViewModel() {

    // Espone il profilo utente come StateFlow per la UI
    val userProfile: StateFlow<UserProfile> = profileManager.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile() // Parte con i default definiti nella data class
        )

    // Funzione per aggiornare i dati nel DataStore
    fun updateProfile(newProfile: UserProfile) {
        viewModelScope.launch {
            profileManager.saveUserProfile(newProfile)
        }
    }

    /**
     * Calcola l'obiettivo idrico in base ai dati del profilo.
     * Formula base: 35ml per ogni kg di peso, con piccoli aggiustamenti.
     */
    fun calculateGoal(profile: UserProfile): Int {
        val baseGoal = profile.weightKg * 35
        val activityBonus = (profile.activityLevel - 1.0f) * 500 // Bonus per attività
        val genderBonus = if (profile.isMale) 200 else 0

        return (baseGoal + activityBonus + genderBonus).toInt()
    }
}