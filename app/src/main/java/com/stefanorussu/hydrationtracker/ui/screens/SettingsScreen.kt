package com.stefanorussu.hydrationtracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stefanorussu.hydrationtracker.data.local.ThemePreferencesManager
import com.stefanorussu.hydrationtracker.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    // Osserviamo il tema corrente salvato nel DataStore
    val currentTheme by viewModel.themeMode.collectAsState()

    // Palette colori coerente con la Home
    val DeepNavy = Color(0xFF1A1C1E)
    val SoftSlate = Color(0xFF5E6266)

    val options = listOf(
        ThemePreferencesManager.ThemeMode.SYSTEM to "Segui sistema",
        ThemePreferencesManager.ThemeMode.LIGHT to "Tema chiaro",
        ThemePreferencesManager.ThemeMode.DARK to "Tema scuro"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IMPOSTAZIONI", style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp, color = SoftSlate) },
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
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ASPETTO",
                style = MaterialTheme.typography.labelSmall,
                color = SoftSlate.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gruppo di opzioni selezionabili
            Column(Modifier.selectableGroup()) {
                options.forEach { (mode, label) ->
                    val isSelected = currentTheme == mode

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { viewModel.setThemeMode(mode) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null // Il click è gestito dalla Row
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) DeepNavy else SoftSlate
                        )
                    }
                }
            }
        }
    }
}