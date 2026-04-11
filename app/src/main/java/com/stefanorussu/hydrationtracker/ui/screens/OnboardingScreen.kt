package com.stefanorussu.hydrationtracker.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.stefanorussu.hydrationtracker.data.local.ActivityLevel
import com.stefanorussu.hydrationtracker.data.local.UserProfile
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    profileViewModel: ProfileViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var editWeight by remember { mutableStateOf(" ") }
    var editBirthDate by remember { mutableStateOf(" ") }

    var editIsMale by remember { mutableStateOf(true) }
    var editActivity by remember { mutableStateOf(ActivityLevel.MODERATE) }

    // Tre stati per gestire perfettamente l'esperienza utente
    var isWaitingForLogin by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var fitbitConnected by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val coroutineScope = rememberCoroutineScope()
    val database = remember { com.stefanorussu.hydrationtracker.data.local.AppDatabase.getDatabase(context) }
    val fitbitRepo = remember { com.stefanorussu.hydrationtracker.data.repository.FitbitRepository(context, database.waterDao()) }

    // --- LA MAGIA: IL SENSORE DI RITORNO NELL'APP ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Se l'app si "risveglia" (ON_RESUME) e stavamo aspettando il login...
            if (event == Lifecycle.Event.ON_RESUME && isWaitingForLogin) {
                isWaitingForLogin = false // Smettiamo di aspettare
                isSyncing = true // Facciamo partire la rotellina

                coroutineScope.launch {
                    delay(500) // Pausa di mezzo secondo per dare tempo all'app di salvare la chiave

                    val token = context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE).getString("access_token", null)

                    if (token != null) {
                        // AUTOSCARICAMENTO!
                        val profile = fitbitRepo.fetchFitbitProfile()
                        if (profile != null) {
                            editWeight = profile.weight.toString()

                            val parts = profile.dateOfBirth.split("-")
                            if (parts.size == 3) {
                                editBirthDate = "${parts[2]}-${parts[1]}-${parts[0]}"
                            }

                            editIsMale = profile.gender.equals("MALE", ignoreCase = true)
                            fitbitConnected = true // Cambia l'aspetto del bottone!
                            Toast.makeText(context, "Dati prelevati in automatico!", Toast.LENGTH_LONG).show()
                        }
                    }
                    isSyncing = false // Spegne la rotellina
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        editBirthDate = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Benvenuto! \uD83D\uDCA7",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Per calcolare il tuo obiettivo di idratazione ideale, abbiamo bisogno di qualche dato. Puoi inserirli a mano o importarli automaticamente.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // IL BOTTONE INTELLIGENTE
            Button(
                onClick = {
                    val token = context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE).getString("access_token", null)

                    if (token == null) {
                        Toast.makeText(context, "Apertura pagina di Login...", Toast.LENGTH_SHORT).show()

                        val clientId = com.stefanorussu.hydrationtracker.BuildConfig.FITBIT_CLIENT_ID
                        val redirectUri = com.stefanorussu.hydrationtracker.BuildConfig.FITBIT_REDIRECT_URI

                        val authUrl = "https://www.fitbit.com/oauth2/authorize?response_type=code&client_id=$clientId&redirect_uri=$redirectUri&scope=activity%20profile%20weight%20nutrition"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                        context.startActivity(intent)

                        isWaitingForLogin = true // Attiva il "sensore di ritorno"
                    } else {
                        // Se aveva già il token in memoria da prove precedenti
                        isSyncing = true
                        coroutineScope.launch {
                            val profile = fitbitRepo.fetchFitbitProfile()
                            if (profile != null) {
                                editWeight = profile.weight.toString()
                                val parts = profile.dateOfBirth.split("-")
                                if (parts.size == 3) {
                                    editBirthDate = "${parts[2]}-${parts[1]}-${parts[0]}"
                                }
                                editIsMale = profile.gender.equals("MALE", ignoreCase = true)
                                fitbitConnected = true
                            }
                            isSyncing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (fitbitConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (fitbitConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                enabled = !isWaitingForLogin && !isSyncing && !fitbitConnected // Si disattiva se ha già finito!
            ) {
                if (isSyncing || isWaitingForLogin) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isWaitingForLogin) "In attesa..." else "Scaricamento...", fontWeight = FontWeight.Bold)
                } else if (fitbitConnected) {
                    Icon(Icons.Default.Check, contentDescription = "Fatto", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dati Prelevati con Successo", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connetti a Fitbit (Opzionale)", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceVariant)
                Text(" OPPURE ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = editWeight,
                    onValueChange = { editWeight = it },
                    label = { Text("Peso (kg)") },
                    leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = editBirthDate,
                        onValueChange = { },
                        label = { Text("Data", maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Sesso", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = editIsMale,
                    onClick = { editIsMale = true },
                    label = { Text("Uomo", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = !editIsMale,
                    onClick = { editIsMale = false },
                    label = { Text("Donna", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Livello di Attività", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = editActivity == ActivityLevel.LOW,
                    onClick = { editActivity = ActivityLevel.LOW },
                    label = { Text("Sedentario", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = editActivity == ActivityLevel.MODERATE,
                    onClick = { editActivity = ActivityLevel.MODERATE },
                    label = { Text("Moderato", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = editActivity == ActivityLevel.HIGH,
                    onClick = { editActivity = ActivityLevel.HIGH },
                    label = { Text("Attivo", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val cleanWeight = editWeight.trim().replace(",", ".")
                    val newWeight = cleanWeight.toFloatOrNull() ?: 70f
                    val cleanBirthDate = editBirthDate.trim()

                    val newProfile = UserProfile(
                        weightKg = newWeight,
                        birthDate = cleanBirthDate,
                        isMale = editIsMale,
                        activityLevel = editActivity
                    )

                    profileViewModel.updateProfile(context, newProfile)

                    val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("is_first_run", false).apply()

                    onFinish()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = editWeight.trim().isNotEmpty() && editBirthDate.trim().isNotEmpty()
            ) {
                Text("CALCOLA E INIZIA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}