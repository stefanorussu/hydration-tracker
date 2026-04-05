package com.stefanorussu.hydrationtracker.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.stefanorussu.hydrationtracker.ui.screens.HomeScreen
import com.stefanorussu.hydrationtracker.ui.screens.SettingsScreen
import com.stefanorussu.hydrationtracker.ui.screens.ProfileScreen
import com.stefanorussu.hydrationtracker.ui.viewmodel.SettingsViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.WaterViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel

object Screen {
    const val Home = "home"
    const val Settings = "settings"
    const val Profile = "profile"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    waterViewModel: WaterViewModel,
    settingsViewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel // <--- AGGIUNTO QUESTO PARAMETRO
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        // Schermata Home
        composable(Screen.Home) {
            HomeScreen(
                waterViewModel = waterViewModel,
                profileViewModel = profileViewModel, // <--- PASSA IL VIEWMODEL ALLA HOME
                onSettingsClick = { navController.navigate(Screen.Settings) },
                onProfileClick = { navController.navigate(Screen.Profile) } // <--- NAVIGAZIONE AL PROFILO
            )
        }

        // Schermata Impostazioni
        composable(Screen.Settings) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Schermata Profilo
        composable(Screen.Profile) {
            ProfileScreen(
                viewModel = profileViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}