package com.stefanorussu.hydrationtracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.stefanorussu.hydrationtracker.ui.screens.HomeScreen
import com.stefanorussu.hydrationtracker.ui.screens.ProfileScreen
import com.stefanorussu.hydrationtracker.ui.screens.SettingsScreen
import com.stefanorussu.hydrationtracker.ui.screens.StatsScreen
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.SettingsViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.StatsViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.WaterViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    waterViewModel: WaterViewModel,
    settingsViewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel,
    statsViewModel: StatsViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentRoute == "home",
                    onClick = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Statistiche") },
                    label = { Text("Statistiche") },
                    selected = currentRoute == "stats",
                    onClick = {
                        navController.navigate("stats") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Impostazioni") },
                    label = { Text("Impostazioni") },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {

            composable("home") {
                HomeScreen(
                    waterViewModel = waterViewModel,
                    profileViewModel = profileViewModel
                )
            }

            composable("stats") {
                StatsScreen(
                    viewModel = statsViewModel,
                    profileViewModel = profileViewModel, // <-- Aggiunto per passare i dati al grafico
                    onBackClick = { navController.navigateUp() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    profileViewModel = profileViewModel,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToProfile = { navController.navigate("profile") }
                )
            }

            composable("profile") {
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}