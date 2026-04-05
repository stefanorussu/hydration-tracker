package com.stefanorussu.hydrationtracker // Assicurati che il package sia corretto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.stefanorussu.hydrationtracker.data.local.AppDatabase
import com.stefanorussu.hydrationtracker.data.local.ThemePreferencesManager
import com.stefanorussu.hydrationtracker.data.local.UserProfileManager
import com.stefanorussu.hydrationtracker.data.repository.WaterRepository
import com.stefanorussu.hydrationtracker.ui.NavGraph
import com.stefanorussu.hydrationtracker.ui.theme.HydrationTrackerTheme
import com.stefanorussu.hydrationtracker.ui.viewmodel.*
import com.stefanorussu.hydrationtracker.worker.HydrationReminderWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inizializzazione Manager e Repository
        val database = AppDatabase.getDatabase(this)
        val repository = WaterRepository(database.waterDao())
        val themePrefsManager = ThemePreferencesManager(applicationContext)
        val userProfileManager = UserProfileManager(applicationContext)

        // 2. Notifiche e Permessi
        val workRequest = PeriodicWorkRequestBuilder<HydrationReminderWorker>(2, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "HydrationReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101
            )
        }

        // 3. UI
        setContent {
            // SettingsViewModel non ha bisogno di Factory complessa se non ha parametri particolari,
            // ma per coerenza usiamo lo stesso approccio o passiamo il manager.
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(themePrefsManager)
            )
            val themeMode by settingsViewModel.themeMode.collectAsState()

            if (themeMode != null) {
                val useDarkTheme = when (themeMode) {
                    ThemePreferencesManager.ThemeMode.LIGHT -> false
                    ThemePreferencesManager.ThemeMode.DARK -> true
                    else -> isSystemInDarkTheme()
                }

                HydrationTrackerTheme(darkTheme = useDarkTheme) {
                    val navController = rememberNavController()

                    val waterViewModel: WaterViewModel = viewModel(
                        factory = WaterViewModelFactory(repository)
                    )

                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModelFactory(userProfileManager)
                    )

                    NavGraph(
                        navController = navController,
                        waterViewModel = waterViewModel,
                        settingsViewModel = settingsViewModel,
                        profileViewModel = profileViewModel
                    )
                }
            }
        }
    }
}