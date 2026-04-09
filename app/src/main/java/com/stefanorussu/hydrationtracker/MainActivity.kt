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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.stefanorussu.hydrationtracker.data.backup.BackupRepository
import com.stefanorussu.hydrationtracker.data.backup.CryptoManager
import com.stefanorussu.hydrationtracker.data.backup.GoogleDriveManager
import com.stefanorussu.hydrationtracker.data.local.AppDatabase
import com.stefanorussu.hydrationtracker.data.local.BackupPreferencesManager
import com.stefanorussu.hydrationtracker.data.local.ThemePreferencesManager
import com.stefanorussu.hydrationtracker.data.local.UserProfileManager
import com.stefanorussu.hydrationtracker.data.network.FitbitAuthManager
import com.stefanorussu.hydrationtracker.data.repository.FitbitRepository
import com.stefanorussu.hydrationtracker.data.repository.WaterRepository
import com.stefanorussu.hydrationtracker.ui.NavGraph
import com.stefanorussu.hydrationtracker.ui.screens.OnboardingScreen
import com.stefanorussu.hydrationtracker.ui.theme.HydrationTrackerTheme
import com.stefanorussu.hydrationtracker.ui.viewmodel.*
import com.stefanorussu.hydrationtracker.ui.GlobalSnackbarProvider
import com.stefanorussu.hydrationtracker.ui.LocalSnackbarHostState
import com.stefanorussu.hydrationtracker.worker.HydrationReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private var pendingFitbitCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri != null && uri.scheme == "hydrationtracker" && uri.host == "callback") {
            pendingFitbitCode = uri.getQueryParameter("code")
        }

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
        val fitbitRepository = FitbitRepository(context, waterDao)
        val fitbitAuthManager = FitbitAuthManager(this)

        // --- ATTIVAZIONE WORKMANAGER (NOTIFICHE BACKGROUND) ---
        val workRequest = PeriodicWorkRequestBuilder<HydrationReminderWorker>(2, TimeUnit.HOURS)
            .setInitialDelay(1, TimeUnit.HOURS) // Aspetta un'ora prima del primo controllo
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "SmartHydrationWorker",
            ExistingPeriodicWorkPolicy.KEEP, // Se esiste già, non sovrascriverlo per non sprecare batteria
            workRequest
        )
        // ------------------------------------------------------

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(themePrefsManager, backupRepository, backupPrefsManager, driveManager))
            val waterViewModel: WaterViewModel = viewModel(factory = WaterViewModelFactory(waterRepository, fitbitRepository))
            val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(userProfileManager))
            val statsViewModel: StatsViewModel = viewModel(factory = StatsViewModelFactory(waterRepository))

            val themeMode by settingsViewModel.themeMode.collectAsState()

            if (themeMode != null) {
                val useDarkTheme = when (themeMode) {
                    ThemePreferencesManager.ThemeMode.LIGHT -> false
                    ThemePreferencesManager.ThemeMode.DARK -> true
                    else -> isSystemInDarkTheme()
                }

                HydrationTrackerTheme(darkTheme = useDarkTheme) {
                    GlobalSnackbarProvider {
                        val snackbarHost = LocalSnackbarHostState.current
                        val coroutineScope = rememberCoroutineScope()
                        val localContext = LocalContext.current
                        val prefs = localContext.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)

                        var isFirstRun by remember { mutableStateOf(prefs.getBoolean("is_first_run", true)) }

                        androidx.compose.runtime.LaunchedEffect(pendingFitbitCode) {
                            pendingFitbitCode?.let { code ->
                                snackbarHost.showSnackbar("Collegamento a Fitbit in corso...")
                                val success = withContext(Dispatchers.IO) { fitbitAuthManager.exchangeCodeForToken(code) }
                                if (success) {
                                    snackbarHost.showSnackbar("Fitbit collegato con successo! 🎉")
                                } else {
                                    snackbarHost.showSnackbar("Errore durante il collegamento. ⚠️")
                                }
                                pendingFitbitCode = null
                            }
                        }

                        if (isFirstRun) {
                            OnboardingScreen(
                                profileViewModel = profileViewModel,
                                onFinish = { isFirstRun = false }
                            )
                        } else {
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
}