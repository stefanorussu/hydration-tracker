package com.stefanorussu.hydrationtracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefanorussu.hydrationtracker.data.local.ActivityLevel
import com.stefanorussu.hydrationtracker.data.local.UserProfile
import com.stefanorussu.hydrationtracker.data.local.UserProfileManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

class ProfileViewModel(private val profileManager: UserProfileManager) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = profileManager.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            // CORREZIONE 1: Aggiunti i valori di default per evitare l'errore di classe vuota
            initialValue = UserProfile(
                weightKg = 70f,
                birthDate = "2000-01-01",
                isMale = true,
                activityLevel = ActivityLevel.MODERATE
            )
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
        if (currentProfile.weightKg != newWeight) {
            val updatedProfile = currentProfile.copy(weightKg = newWeight)
            updateProfile(context, updatedProfile)
            return true
        }
        return false
    }

    fun calculateGoal(profile: UserProfile): Int {
        val baseGoal = profile.weightKg * profile.activityLevel.multiplier
        val genderBonus = if (profile.isMale) 200 else 0

        // CORREZIONE 2: Calcoliamo dinamicamente l'età dalla data di nascita
        val currentAge = calculateAgeFromDOB(profile.birthDate)

        val ageAdjustment = when {
            currentAge < 30 -> 100
            currentAge > 60 -> -100
            else -> 0
        }

        return (baseGoal + genderBonus + ageAdjustment).toInt()
    }

    fun calculateAgeFromDOB(birthDate: String): Int {
        return try {
            val dob = LocalDate.parse(birthDate, DateTimeFormatter.ISO_DATE) // Legge "YYYY-MM-DD"
            val currentDate = LocalDate.now()
            Period.between(dob, currentDate).years
        } catch (e: Exception) {
            25 // Età di default se la data è formattata male
        }
    }

    fun formatToReadableDate(inputDate: String): String {
        if (inputDate.isEmpty()) return "Data non inserita"

        // Proviamo i diversi formati possibili per evitare crash con i vecchi dati
        val formats = listOf("dd-MM-yyyy", "yyyy-MM-dd")

        for (format in formats) {
            try {
                val parsedDate = LocalDate.parse(inputDate, DateTimeFormatter.ofPattern(format))
                return parsedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ITALIAN))
            } catch (e: Exception) {
                continue // Prova il prossimo formato
            }
        }
        return inputDate // Se fallisce tutto, mostra quello che c'è
    }
}