package com.stefanorussu.hydrationtracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stefanorussu.hydrationtracker.data.local.UserProfile
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()

    // Stati locali per gestire l'input prima del salvataggio
    var weightInput by remember(profile) { mutableStateOf(profile.weightKg.toString()) }
    var isMale by remember(profile) { mutableStateOf(profile.isMale) }

    val SoftSlate = Color(0xFF5E6266)
    val DeepNavy = Color(0xFF1A1C1E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PROFILO", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, color = SoftSlate) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = SoftSlate)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // SEZIONE PESO
            Column {
                Text("PESO (KG)", style = MaterialTheme.typography.labelSmall, color = SoftSlate)
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // SEZIONE SESSO
            Column {
                Text("SESSO", style = MaterialTheme.typography.labelSmall, color = SoftSlate)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isMale, onClick = { isMale = true })
                    Text("Uomo", modifier = Modifier.padding(end = 16.dp))
                    RadioButton(selected = !isMale, onClick = { isMale = false })
                    Text("Donna")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // TASTO SALVA
            Button(
                onClick = {
                    val weight = weightInput.toFloatOrNull() ?: profile.weightKg
                    viewModel.updateProfile(profile.copy(weightKg = weight, isMale = isMale))
                    onBackClick() // Torna indietro dopo il salvataggio
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("SALVA MODIFICHE", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}