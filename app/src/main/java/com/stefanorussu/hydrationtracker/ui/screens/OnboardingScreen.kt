package com.stefanorussu.hydrationtracker.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
fun OnboardingScreen(
    profileViewModel: ProfileViewModel,
    onFinish: () -> Unit
) {
    val context = LocalContext.current

    // Stati iniziali vuoti per invitare l'utente a compilarli
    var editWeight by remember { mutableStateOf("") }
    var editAge by remember { mutableStateOf("") }
    var editIsMale by remember { mutableStateOf(true) }
    var editActivity by remember { mutableStateOf(ActivityLevel.MODERATE) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Benvenuto! \uD83D\uDCA7",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Per calcolare il tuo obiettivo di idratazione ideale su misura per te, abbiamo bisogno di qualche dato.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // SESSO
            Text("Sesso", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
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

            Spacer(modifier = Modifier.height(24.dp))

            // LIVELLO DI ATTIVITÀ
            Text("Livello di Attività", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
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
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { editActivity = level },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = level.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // PULSANTE FINALE DI SALVATAGGIO (Corretto)
            Button(
                onClick = {
                    val newWeight = editWeight.replace(",", ".").toFloatOrNull() ?: 70f
                    val newAge = editAge.toIntOrNull() ?: 25

                    val newProfile = UserProfile(
                        weightKg = newWeight,
                        age = newAge,
                        isMale = editIsMale,
                        activityLevel = editActivity
                    )

                    profileViewModel.updateProfile(context, newProfile)

                    val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("is_first_run", false).apply()

                    onFinish()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp), // ← Modificatore corretto
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("CALCOLA E INIZIA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Spazio esterno in fondo per evitare che il pulsante si attacchi al bordo schermo
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}