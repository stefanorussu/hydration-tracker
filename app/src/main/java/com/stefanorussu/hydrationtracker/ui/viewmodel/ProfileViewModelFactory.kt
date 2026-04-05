package com.stefanorussu.hydrationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stefanorussu.hydrationtracker.data.local.UserProfileManager

class ProfileViewModelFactory(private val profileManager: UserProfileManager) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(profileManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}