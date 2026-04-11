package com.stefanorussu.hydrationtracker.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stefanorussu.hydrationtracker.data.local.ThemePreferencesManager
import com.stefanorussu.hydrationtracker.data.network.FitbitAuthManager
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.SettingsViewModel
import com.stefanorussu.hydrationtracker.ui.LocalSnackbarHostState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.rotate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel,
    onBackClick: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by settingsViewModel.themeMode.collectAsState()
    val profile by profileViewModel.userProfile.collectAsState()
    val dailyGoal = profileViewModel.calculateGoal(profile)

    val isLocalBackupEnabled by settingsViewModel.isLocalBackupEnabled.collectAsState()

    val globalSnackbar = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    val fitbitAuthManager = remember { FitbitAuthManager(context) }
    var isFitbitLinked by remember { mutableStateOf(fitbitAuthManager.isLinked()) }
    var isSyncing by remember { mutableStateOf(false) } // Nuova variabile per l'animazione di caricamento
    var lastSyncTime by remember { mutableStateOf(context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE).getString("last_sync_time", "Mai")) }

    val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)
    var isNotificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var selectedPauseOption by remember { mutableStateOf(prefs.getString("pause_option", "Per sempre") ?: "Per sempre") }
    var showPauseOptions by remember { mutableStateOf(false) }

    var silenceStart by remember { mutableStateOf(prefs.getString("silence_start", "23:00") ?: "23:00") }
    var silenceEnd by remember { mutableStateOf(prefs.getString("silence_end", "07:00") ?: "07:00") }

    // Leggiamo l'orario di riattivazione
    val pauseUntil = remember { mutableStateOf(prefs.getLong("notifications_pause_until", 0L)) }

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var pickingStartTime by remember { mutableStateOf(true) } // Per capire se stiamo cambiando l'inizio o la fine
    val timePickerState = androidx.compose.material3.rememberTimePickerState(
        initialHour = 23,
        initialMinute = 0,
        is24Hour = true
    )

    // Funzione rapida per formattare l'orario
    val formatTime = { millis: Long ->
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(millis))
    }

    LaunchedEffect(selectedPauseOption) {
        if (!isNotificationsEnabled && selectedPauseOption != "Per sempre" && showPauseOptions) {
            kotlinx.coroutines.delay(3000)
            showPauseOptions = false
        }
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        scrollState.scrollTo(0)
    }

    val backupMessage by settingsViewModel.backupMessage.collectAsState()
    LaunchedEffect(backupMessage) {
        backupMessage?.let {
            globalSnackbar.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            settingsViewModel.clearBackupMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { settingsViewModel.exportBackup(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { settingsViewModel.importBackup(it) }
    }

    val googleSignInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, googleSignInOptions) }

    val driveBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            task.addOnSuccessListener { account ->
                settingsViewModel.backupToGoogleDrive(account)
            }
        }
    }

    val driveRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            task.addOnSuccessListener { account ->
                settingsViewModel.restoreFromGoogleDrive(account)
            }
        }
    }

    // Funzione per mostrare il selettore orario
    fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val parts = currentTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        android.app.TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            },
            hour,
            minute,
            true // true per il formato 24 ore
        ).show()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Impostazioni", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSection(title = "Profilo e Obiettivo", icon = Icons.Default.Person) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Obiettivo: $dailyGoal ml",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Calcolato in base ai tuoi dati",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onNavigateToProfile) {
                        Text("Visualizza", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // SEZIONE FITBIT AGGIORNATA
            SettingsSection(title = "Dispositivi Esterni", icon = Icons.Default.Watch) {
                if (isFitbitLinked) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Connesso e sincronizzazione attiva",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                // ECCO LA NUOVA SCRITTA!
                                Text(
                                    "Ultimo aggiornamento: $lastSyncTime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isSyncing = true
                                    coroutineScope.launch {
                                        try {
                                            val database = com.stefanorussu.hydrationtracker.data.local.AppDatabase.getDatabase(context)
                                            val fitbitRepo = com.stefanorussu.hydrationtracker.data.repository.FitbitRepository(context, database.waterDao())

                                            val fitbitProfile = fitbitRepo.fetchFitbitProfile()
                                            if (fitbitProfile != null) {
                                                val newWeight = fitbitProfile.weight.toFloat()

                                                if (profile.weightKg != newWeight) {
                                                    val updatedProfile = profile.copy(weightKg = newWeight)
                                                    profileViewModel.updateProfile(context, updatedProfile)
                                                    globalSnackbar.showSnackbar("Profilo aggiornato: nuovo peso rilevato!")
                                                } else {
                                                    globalSnackbar.showSnackbar("Tutti i dati sono già aggiornati.")
                                                }

                                                // Aggiorniamo l'orario anche qui se fa il tap manuale!
                                                val newTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                                                context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE).edit().putString("last_sync_time", newTime).apply()
                                                lastSyncTime = newTime

                                            } else {
                                                globalSnackbar.showSnackbar("Errore di comunicazione con i server Fitbit.")
                                            }
                                        } catch (e: Exception) {
                                            globalSnackbar.showSnackbar("Errore imprevisto durante l'aggiornamento.")
                                        } finally {
                                            isSyncing = false
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                enabled = !isSyncing
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Aggiorna Ora", fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                                    isFitbitLinked = false
                                    coroutineScope.launch {
                                        globalSnackbar.showSnackbar("Fitbit scollegato con successo")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Scollega", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fitbit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Sincronizza peso, sport e idratazione",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                val url = fitbitAuthManager.getLoginUrl()
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Collega", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            SettingsSection(title = "Notifiche", icon = Icons.Default.NotificationsActive) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Raggruppiamo Freccia + Testo a sinistra
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                // Click senza effetto grigio (Ripple)
                                .clickable(
                                    enabled = !isNotificationsEnabled,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null // Questo elimina lo sfondo grigio al tocco
                                ) {
                                    showPauseOptions = !showPauseOptions
                                }
                        ) {
                            if (!isNotificationsEnabled) {
                                val rotationAngle by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = if (showPauseOptions) 180f else 0f,
                                    label = "ArrowRotation"
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.rotate(rotationAngle),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }

                            Column {
                                Text(
                                    text = "Avvisi di Idratazione",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )

                                val statusText = if (isNotificationsEnabled) {
                                    "Attivi"
                                } else if (selectedPauseOption == "Per sempre") {
                                    "Disattivate"
                                } else {
                                    "In pausa fino alle ${formatTime(pauseUntil.value)}"
                                }

                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isNotificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Lo Switch rimane invariato a destra
                        Switch(
                            checked = isNotificationsEnabled,
                            onCheckedChange = { checked ->
                                isNotificationsEnabled = checked
                                if (!checked) showPauseOptions = true
                                else {
                                    showPauseOptions = false
                                    prefs.edit().putLong("notifications_pause_until", 0L).apply()
                                    pauseUntil.value = 0L
                                }
                                prefs.edit().putBoolean("notifications_enabled", checked).apply()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Silenzio Notturno",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "L'algoritmo non invierà avvisi in questa fascia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Tasto Orario Inizio
                            TimePickerChip(
                                label = "Dalle",
                                time = silenceStart,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pickingStartTime = true
                                    showTimePickerDialog = true
                                }
                            )

                            // Tasto Orario Fine
                            TimePickerChip(
                                label = "Alle",
                                time = silenceEnd,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pickingStartTime = false
                                    showTimePickerDialog = true
                                }
                            )
                        }
                    }

                    // --- ANIMAZIONE A CASSETTO (Slide + Fade) ---
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showPauseOptions,
                        enter = androidx.compose.animation.expandVertically(expandFrom = Alignment.Top) +
                                androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically(shrinkTowards = Alignment.Top) +
                                androidx.compose.animation.fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Silenzia per:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // I tuoi pulsanti PauseOptionChip rimangono gli stessi
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PauseOptionChip("1 Ora", selectedPauseOption == "1 Ora", modifier = Modifier.weight(1f)) {
                                        selectedPauseOption = "1 Ora"
                                        val time = System.currentTimeMillis() + (1 * 3600 * 1000)
                                        pauseUntil.value = time
                                        prefs.edit().putString("pause_option", "1 Ora").putLong("notifications_pause_until", time).apply()
                                    }
                                    PauseOptionChip("8 Ore", selectedPauseOption == "8 Ore", modifier = Modifier.weight(1f)) {
                                        selectedPauseOption = "8 Ore"
                                        val time = System.currentTimeMillis() + (8 * 3600 * 1000)
                                        pauseUntil.value = time
                                        prefs.edit().putString("pause_option", "8 Ore").putLong("notifications_pause_until", time).apply()
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PauseOptionChip("Domani", selectedPauseOption == "Fino a domani", modifier = Modifier.weight(1f)) {
                                        selectedPauseOption = "Fino a domani"
                                        val calendar = java.util.Calendar.getInstance().apply {
                                            add(java.util.Calendar.DAY_OF_YEAR, 1)
                                            set(java.util.Calendar.HOUR_OF_DAY, 8)
                                            set(java.util.Calendar.MINUTE, 0)
                                        }
                                        pauseUntil.value = calendar.timeInMillis
                                        prefs.edit().putString("pause_option", "Fino a domani").putLong("notifications_pause_until", calendar.timeInMillis).apply()
                                    }
                                    PauseOptionChip("Sempre", selectedPauseOption == "Per sempre", modifier = Modifier.weight(1f)) {
                                        selectedPauseOption = "Per sempre"
                                        pauseUntil.value = 0L
                                        prefs.edit().putString("pause_option", "Per sempre").putLong("notifications_pause_until", 0L).apply()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SettingsSection(title = "Aspetto", icon = Icons.Default.Palette) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeSelectionChip(
                        title = "Sistema",
                        selected = currentTheme == ThemePreferencesManager.ThemeMode.SYSTEM,
                        onClick = { settingsViewModel.setThemeMode(ThemePreferencesManager.ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeSelectionChip(
                        title = "Chiaro",
                        selected = currentTheme == ThemePreferencesManager.ThemeMode.LIGHT,
                        onClick = { settingsViewModel.setThemeMode(ThemePreferencesManager.ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeSelectionChip(
                        title = "Scuro",
                        selected = currentTheme == ThemePreferencesManager.ThemeMode.DARK,
                        onClick = { settingsViewModel.setThemeMode(ThemePreferencesManager.ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SettingsSection(title = "Dati e Backup", icon = Icons.Default.Storage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Backup Automatico",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Salva regolarmente sul dispositivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isLocalBackupEnabled,
                        onCheckedChange = { settingsViewModel.toggleLocalBackup(it) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { exportLauncher.launch("HydrationTracker_Backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Backup Locale", textAlign = TextAlign.Center)
                    }
                    FilledTonalButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ripristina", textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { driveBackupLauncher.launch(googleSignInClient.signInIntent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Esegui backup su Drive", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { driveRestoreLauncher.launch(googleSignInClient.signInIntent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ripristina da Drive", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🔒 Privacy garantita: Database locale. Nessun dato lascia il telefono senza il tuo permesso.",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showTimePickerDialog) {
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val formattedTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    if (pickingStartTime) {
                        silenceStart = formattedTime
                        prefs.edit().putString("silence_start", formattedTime).apply()
                    } else {
                        silenceEnd = formattedTime
                        prefs.edit().putString("silence_end", formattedTime).apply()
                    }
                    showTimePickerDialog = false
                }) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) { Text("Annulla") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (pickingStartTime) "Inizio silenzio" else "Fine silenzio",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionChip(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauseOptionChip(title: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun TimePickerChip(label: String, time: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                time,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}