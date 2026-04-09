package com.stefanorussu.hydrationtracker.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

    // --- FIX SCORRIMENTO ---
    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        scrollState.scrollTo(0) // Torna sempre in cima quando apri il tab!
    }
    // -----------------------

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

    Scaffold(
        topBar = {
            // Sostituisci "TopAppBar" con "CenterAlignedTopAppBar"
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
                        Text("Modifica", fontWeight = FontWeight.Bold)
                    }
                }
            }

            SettingsSection(title = "Dispositivi Esterni", icon = Icons.Default.Watch) {
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
                            text = if (isFitbitLinked) "Sincronizzazione attiva" else "Sincronizza peso e idratazione",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFitbitLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isFitbitLinked) {
                        Button(
                            onClick = {
                                context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                                isFitbitLinked = false
                                coroutineScope.launch {
                                    globalSnackbar.showSnackbar("Fitbit scollegato con successo")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Scollega", fontWeight = FontWeight.Bold)
                        }
                    } else {
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