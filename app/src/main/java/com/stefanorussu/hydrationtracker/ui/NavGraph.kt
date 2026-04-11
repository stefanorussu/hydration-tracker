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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination?.route

                val items = listOf(
                    Triple("Home", Icons.Outlined.WaterDrop, Icons.Default.WaterDrop) to "home",
                    Triple("Statistiche", Icons.Outlined.Timeline, Icons.Default.Timeline) to "stats",
                    Triple("Impostazioni", Icons.Outlined.Settings, Icons.Default.Settings) to "settings"
                )

                items.forEach { (info, route) ->
                    val (label, outlinedIcon, filledIcon) = info
                    val isSelected = currentDestination == route

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentDestination != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        label = { Text(label) },
                        icon = {
                            // 1. Rotazione per Settings
                            val animatedRotation by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isSelected && route == "settings") 90f else 0f,
                                animationSpec = androidx.compose.animation.core.tween(durationMillis = 400),
                                label = "SettingsRotation"
                            )

                            // 2. Rimbalzo per Home
                            val animatedBounce by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isSelected && route == "home") 1.2f else 1f,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                ),
                                label = "HomeBounce"
                            )

                            // 3. Traslazione per Stats
                            val animatedShiftY by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isSelected && route == "stats") -8f else 0f,
                                animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
                                label = "StatsTranslation"
                            )

                            Icon(
                                imageVector = if (isSelected) filledIcon else outlinedIcon,
                                contentDescription = label,
                                modifier = androidx.compose.ui.Modifier.graphicsLayer {
                                    // Usiamo i nomi delle variabili animate per impostare le proprietà
                                    when (route) {
                                        "settings" -> rotationZ = animatedRotation
                                        "home" -> {
                                            scaleX = animatedBounce
                                            scaleY = animatedBounce
                                        }
                                        "stats" -> translationY = animatedShiftY
                                    }
                                }
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            // Colori quando il tab è SELEZIONATO
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,

                            // Colori quando il tab NON è selezionato (fondamentali per la Dark Mode)
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,

                            // Colori per l'effetto di pressione (opzionale)
                            disabledIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    )
                }
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