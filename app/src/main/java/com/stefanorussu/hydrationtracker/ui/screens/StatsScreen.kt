package com.stefanorussu.hydrationtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.StatsViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.TimeTab
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    profileViewModel: ProfileViewModel,
    onBackClick: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val dateRangeText by viewModel.dateRangeText.collectAsState()

    val recordsList by viewModel.currentRecords.collectAsState()
    val listItems by viewModel.listItems.collectAsState()
    val chartItems by viewModel.chartItems.collectAsState()
    val summaryValue by viewModel.summaryValue.collectAsState()

    val profile by profileViewModel.userProfile.collectAsState()
    val dailyGoalMl = profileViewModel.calculateGoal(profile).takeIf { it > 0 } ?: 2000

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val context = LocalContext.current
    val successColor = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF388E3C)

    var recordToEdit by remember { mutableStateOf<WaterRecord?>(null) }

    fun formatK(value: Int): String {
        return if (value >= 1000) {
            val kValue = value / 1000f
            if (kValue % 1f == 0f) String.format(Locale.US, "%.0fk", kValue)
            else String.format(Locale.US, "%.1fk", kValue)
        } else {
            value.toString()
        }
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(title = { Text("Statistiche", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) })
                TabRow(
                    selectedTabIndex = currentTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(selected = currentTab == TimeTab.DAY, onClick = { viewModel.setTab(TimeTab.DAY) }, text = { Text("Giorno") })
                    Tab(selected = currentTab == TimeTab.WEEK, onClick = { viewModel.setTab(TimeTab.WEEK) }, text = { Text("Settimana") })
                    Tab(selected = currentTab == TimeTab.MONTH, onClick = { viewModel.setTab(TimeTab.MONTH) }, text = { Text("Mese") })
                    Tab(selected = currentTab == TimeTab.YEAR, onClick = { viewModel.setTab(TimeTab.YEAR) }, text = { Text("Anno") })
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.shiftTime(false) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Precedente", tint = MaterialTheme.colorScheme.onSurface) }
                Text(text = dateRangeText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { viewModel.shiftTime(true) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Successivo", tint = MaterialTheme.colorScheme.onSurface) }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "$summaryValue ml", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(text = if (currentTab == TimeTab.DAY) "Totale in questo giorno" else "Media giornaliera in questo periodo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (summaryValue > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val isGoalReached = summaryValue >= dailyGoalMl
                        Surface(
                            color = if (isGoalReached) successColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isGoalReached) "🎯 Raggiunto" else "⚠️ Sotto obiettivo",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGoalReached) successColor else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (currentTab != TimeTab.DAY) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 16.dp)) {
                    if (chartItems.isEmpty() || chartItems.all { it.value == 0 }) {
                        Text("Nessun dato registrato", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
                    } else {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.width(40.dp).fillMaxHeight().padding(bottom = 24.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(text = formatK(dailyGoalMl), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = formatK(dailyGoalMl / 2), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                Column(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        chartItems.forEach { bar ->
                                            val barHeightPercentage = (bar.value.toFloat() / dailyGoalMl.toFloat()).coerceIn(0f, 1f)
                                            val barColor = if (bar.value >= dailyGoalMl) successColor else MaterialTheme.colorScheme.primary

                                            Box(
                                                modifier = Modifier
                                                    .width(if (currentTab == TimeTab.MONTH) 6.dp else 16.dp)
                                                    .fillMaxHeight(barHeightPercentage)
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                    .background(if (bar.value > 0) barColor else Color.Transparent)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        chartItems.forEach { bar ->
                                            Box(modifier = Modifier.width(if (currentTab == TimeTab.MONTH) 6.dp else 16.dp), contentAlignment = Alignment.Center) {
                                                if (bar.showLabel) {
                                                    Text(
                                                        text = bar.label,
                                                        modifier = Modifier.wrapContentWidth(unbounded = true),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(top = 8.dp))
            Text(text = if (currentTab == TimeTab.DAY) "Cronologia delle bevute" else "Dettagli del periodo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (currentTab == TimeTab.DAY) {
                if (recordsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Nessun dato per questo giorno", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(recordsList) { record ->
                            val dynamicIcon = when (record.drinkName) {
                                "Tè" -> Icons.Default.EmojiFoodBeverage
                                "Caffè" -> Icons.Default.Coffee
                                "Sport Drink" -> Icons.Default.FitnessCenter
                                "Bibita" -> Icons.Default.LocalBar
                                "Latte" -> Icons.Default.LocalCafe
                                "Succo" -> Icons.Default.LocalFlorist
                                else -> Icons.Default.LocalDrink
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { recordToEdit = record }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Icon(imageVector = dynamicIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = record.drinkName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = "Tocca per modificare", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "${record.amountMl} ml", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = timeFormatter.format(Date(record.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                if (listItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Nessun dato per questo periodo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(listItems) { item ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.drillDownTo(item.timestamp, item.targetTab) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(text = item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    if (item.subtitle.isNotEmpty()) {
                                        Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "${item.value} ml", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (item.value >= dailyGoalMl) successColor else MaterialTheme.colorScheme.onSurface)
                                    if (item.isAverage) {
                                        Text(text = "Media giorn.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (recordToEdit != null) {
        var editAmountText by remember { mutableStateOf(recordToEdit!!.amountMl.toString()) }
        var editTimestamp by remember { mutableLongStateOf(recordToEdit!!.timestamp) }

        val dialogDateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
        val dialogTimeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { recordToEdit = null },
            title = { Text(text = "Modifica ${recordToEdit!!.drinkName}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) editAmountText = it },
                        label = { Text("Quantità (ml)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Data e Ora", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = editTimestamp }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newC = Calendar.getInstance().apply { timeInMillis = editTimestamp }
                                        newC.set(year, month, dayOfMonth)
                                        editTimestamp = newC.timeInMillis
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(dialogDateFormatter.format(Date(editTimestamp)))
                        }

                        OutlinedButton(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = editTimestamp }
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val newC = Calendar.getInstance().apply { timeInMillis = editTimestamp }
                                        newC.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        newC.set(Calendar.MINUTE, minute)
                                        editTimestamp = newC.timeInMillis
                                    },
                                    c.get(Calendar.HOUR_OF_DAY),
                                    c.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(dialogTimeFormatter.format(Date(editTimestamp)))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newVal = editAmountText.toIntOrNull() ?: 0
                        if (newVal > 0) {
                            viewModel.updateRecord(recordToEdit!!, newVal, editTimestamp)
                        } else {
                            viewModel.deleteRecord(recordToEdit!!)
                        }
                        recordToEdit = null
                    }
                ) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(recordToEdit!!)
                        recordToEdit = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina")
                }
            }
        )
    }
}