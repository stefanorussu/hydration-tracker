package com.example.myfirstapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myfirstapp.ui.viewmodel.WaterViewModel
import com.example.myfirstapp.model.DrinkType

@Composable
fun HomeScreen(viewModel: WaterViewModel) {
    val logs by viewModel.logs.collectAsState()
    val totalToday by viewModel.todayTotal.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Idratazione") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card del progresso
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Oggi hai bevuto:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${totalToday ?: 0} ml",
                        style = MaterialTheme.typography.displayLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottoni rapidi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { viewModel.addDrink(250, DrinkType.WATER) }) {
                    Text("+250ml Acqua")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lista dei log
            Text(
                text = "Cronologia bevande",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleSmall
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs) { log ->
                    ListItem(
                        headlineContent = { Text("${log.amountMl}ml di ${log.drinkType}") },
                        supportingContent = { Text("Acqua reale: ${log.waterContentMl}ml") },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeLog(log) }) {
                                Text("X") // Sostituiremo con icona dopo
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}