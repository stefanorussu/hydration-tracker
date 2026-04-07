package com.stefanorussu.hydrationtracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.stefanorussu.hydrationtracker.data.backup.BackupRepository
import com.stefanorussu.hydrationtracker.data.backup.CryptoManager
import com.stefanorussu.hydrationtracker.data.backup.GoogleDriveManager
import com.stefanorussu.hydrationtracker.data.local.AppDatabase
import com.stefanorussu.hydrationtracker.data.local.BackupPreferencesManager
import com.stefanorussu.hydrationtracker.data.local.ThemePreferencesManager
import com.stefanorussu.hydrationtracker.data.local.UserProfileManager
import com.stefanorussu.hydrationtracker.data.repository.WaterRepository
import com.stefanorussu.hydrationtracker.ui.NavGraph
import com.stefanorussu.hydrationtracker.ui.screens.OnboardingScreen
import com.stefanorussu.hydrationtracker.ui.theme.HydrationTrackerTheme
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModelFactory
import com.stefanorussu.hydrationtracker.ui.viewmodel.SettingsViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.SettingsViewModelFactory
import com.stefanorussu.hydrationtracker.ui.viewmodel.StatsViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.StatsViewModelFactory
import com.stefanorussu.hydrationtracker.ui.viewmodel.WaterViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.WaterViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = applicationContext
        val database = AppDatabase.getDatabase(context)
        val waterDao = database.waterDao()

        val themePrefsManager = ThemePreferencesManager(context)
        val backupPrefsManager = BackupPreferencesManager(context)
        val userProfileManager = UserProfileManager(context)

        val cryptoManager = CryptoManager()
        val driveManager = GoogleDriveManager(context)

        val backupRepository = BackupRepository(context, waterDao, cryptoManager, driveManager)
        val waterRepository = WaterRepository(waterDao)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(themePrefsManager, backupRepository, backupPrefsManager, driveManager)
            )

            val waterViewModel: WaterViewModel = viewModel(
                factory = WaterViewModelFactory(waterRepository)
            )

            val profileViewModel: ProfileViewModel = viewModel(
                factory = ProfileViewModelFactory(userProfileManager)
            )

            val statsViewModel: StatsViewModel = viewModel(
                factory = StatsViewModelFactory(waterRepository)
            )

            val themeMode by settingsViewModel.themeMode.collectAsState()

            if (themeMode != null) {
                val useDarkTheme = when (themeMode) {
                    ThemePreferencesManager.ThemeMode.LIGHT -> false
                    ThemePreferencesManager.ThemeMode.DARK -> true
                    else -> isSystemInDarkTheme()
                }

                // Passiamo il parametro useDarkTheme per far funzionare il cambio tema
                HydrationTrackerTheme(darkTheme = useDarkTheme) {
                    val localContext = LocalContext.current
                    val prefs = localContext.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)

                    // Legge se è il primo avvio (di default è true se non trova nulla)
                    var isFirstRun by remember { mutableStateOf(prefs.getBoolean("is_first_run", true)) }

                    if (isFirstRun) {
                        OnboardingScreen(
                            profileViewModel = profileViewModel,
                            onFinish = {
                                isFirstRun = false // Cambia lo stato, la UI si aggiorna e passa alla Home
                            }
                        )
                    } else {
                        // VERO CODICE DI NAVIGAZIONE
                        val navController = rememberNavController()
                        NavGraph(
                            navController = navController,
                            waterViewModel = waterViewModel,
                            settingsViewModel = settingsViewModel,
                            profileViewModel = profileViewModel,
                            statsViewModel = statsViewModel
                        )
                    }
                }
            }
        }
    }
}