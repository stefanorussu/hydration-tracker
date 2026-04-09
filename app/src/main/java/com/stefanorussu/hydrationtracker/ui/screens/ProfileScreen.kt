package com.stefanorussu.hydrationtracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stefanorussu.hydrationtracker.data.local.ActivityLevel
import com.stefanorussu.hydrationtracker.data.local.UserProfile
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val profile by profileViewModel.userProfile.collectAsState()
    val context = LocalContext.current

    var editWeight by remember { mutableStateOf(profile.weightKg.toString()) }
    var editAge by remember { mutableStateOf(profile.age.toString()) }
    var editIsMale by remember { mutableStateOf(profile.isMale) }
    var editActivity by remember { mutableStateOf(profile.activityLevel) }

    // --- LOGICA DEL TASTO SALVA INTELLIGENTE ---
    // Controlla se i dati correnti inseriti sono diversi da quelli salvati nel profilo
    val isModified = remember(editWeight, editAge, editIsMale, editActivity, profile) {
        val currentWeight = editWeight.replace(",", ".").toFloatOrNull()
        val currentAge = editAge.toIntOrNull()

        currentWeight != null && currentWeight != profile.weightKg ||
                currentAge != null && currentAge != profile.age ||
                editIsMale != profile.isMale ||
                editActivity != profile.activityLevel
    }
    // -------------------------------------------

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifica Profilo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // PESO E ETÀ
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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

                OutlinedTextField(
                    value = editAge,
                    onValueChange = { if (it.all { c -> c.isDigit() }) editAge = it },
                    label = { Text("Età") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // SESSO (Segmented Control MD3)
            Text("Sesso", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            // LIVELLO DI ATTIVITÀ (Card Interattive Compatte)
            Text("Livello di Attività", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { level ->
                    val isSelected = editActivity == level
                    OutlinedCard(
                        onClick = { editActivity = level },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { editActivity = level },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = level.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val newWeight = editWeight.replace(",", ".").toFloatOrNull() ?: profile.weightKg
                    val newAge = editAge.toIntOrNull() ?: profile.age

                    val newProfile = UserProfile(
                        weightKg = newWeight,
                        age = newAge,
                        isMale = editIsMale,
                        activityLevel = editActivity
                    )

                    profileViewModel.updateProfile(context, newProfile)
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = isModified // IL PULSANTE SI ATTIVA SOLO SE CI SONO MODIFICHE
            ) {
                Text("Salva Profilo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}