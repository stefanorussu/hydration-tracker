package com.stefanorussu.hydrationtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalDrink
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.StatsViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.TimeTab
import com.stefanorussu.hydrationtracker.ui.DrinkCatalog
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

    val isNextEnabled by viewModel.isNextEnabled.collectAsState()

    val profile by profileViewModel.userProfile.collectAsState()
    val dailyGoalMl = profileViewModel.calculateGoal(profile).takeIf { it > 0 } ?: 2000

    val fullDayFormatter = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
    val context = LocalContext.current
    val successColor = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF388E3C)

    var recordToEdit by remember { mutableStateOf<WaterRecord?>(null) }

    val database = remember { com.stefanorussu.hydrationtracker.data.local.AppDatabase.getDatabase(context) }
    val waterDao = remember { database.waterDao() }
    val fitbitRepository = remember { com.stefanorussu.hydrationtracker.data.repository.FitbitRepository(context, waterDao) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.resetToToday()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun formatK(value: Int): String {
        return if (value >= 1000) {
            val kValue = value / 1000f
            if (kValue % 1f == 0f) String.format(Locale.US, "%.0fk", kValue)
            else String.format(Locale.US, "%.1fk", kValue)
        } else {
            value.toString()
        }
    }

    val insightMessage = remember(currentTab, recordsList, chartItems, listItems, summaryValue, dailyGoalMl) {
        fun formatVolume(ml: Int): String {
            return if (ml >= 1000) {
                val liters = ml / 1000f
                if (liters % 1f == 0f) String.format(Locale.US, "%.0f Litri", liters)
                else String.format(Locale.US, "%.1f Litri", liters)
            } else {
                "$ml ml"
            }
        }

        when (currentTab) {
            TimeTab.DAY -> {
                val coffees = recordsList.count { it.drinkName == "Caffè" }
                val lastRecord = recordsList.maxByOrNull { it.timestamp }
                val hoursSinceLast = lastRecord?.let { (System.currentTimeMillis() - it.timestamp) / (1000 * 60 * 60) } ?: 0

                when {
                    summaryValue >= dailyGoalMl -> "Ottimo lavoro, hai raggiunto il tuo obiettivo di idratazione."
                    coffees >= 3 -> "Oggi hai preso $coffees caffè. Ricorda di bilanciare con l'acqua."
                    hoursSinceLast >= 3 && summaryValue > 0 -> "Non bevi da ${hoursSinceLast} ore. È il momento perfetto per un bicchiere."
                    summaryValue >= dailyGoalMl / 2 -> "Sei a buon punto, hai superato la metà del tuo obiettivo giornaliero."
                    summaryValue == 0 -> "Pronto a iniziare? Registra il tuo primo bicchiere della giornata."
                    else -> "Sei sulla strada giusta, continua a idratarti con regolarità."
                }
            }
            TimeTab.WEEK -> {
                val bestDayItem = listItems.filter { !it.isAverage }.maxByOrNull { it.value }
                val daysReached = chartItems.count { it.value >= dailyGoalMl }

                val fullDayName = bestDayItem?.let {
                    fullDayFormatter.format(Date(it.timestamp)).replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                } ?: ""

                when {
                    daysReached == 7 -> "Che settimana! Hai raggiunto l'obiettivo tutti i giorni."
                    daysReached >= 4 -> "Bella costanza, hai raggiunto l'obiettivo $daysReached giorni su 7."
                    bestDayItem != null && bestDayItem.value > 0 -> "Il tuo giorno migliore è stato $fullDayName con ${formatVolume(bestDayItem.value)}."
                    else -> "Ogni giorno conta. Cerca di mantenere il ritmo per migliorare la tua settimana."
                }
            }
            TimeTab.MONTH -> {
                val daysReached = chartItems.count { it.value >= dailyGoalMl && it.value > 0 }
                val totalVolume = chartItems.sumOf { it.value }

                when {
                    daysReached >= 20 -> "Mese fantastico! Hai raggiunto l'obiettivo per ben $daysReached giorni."
                    totalVolume > 0 -> "Fino ad ora, questo mese hai bevuto un totale di ${formatVolume(totalVolume)}."
                    else -> "Un nuovo mese è iniziato. Ricomincia a tracciare la tua idratazione."
                }
            }
            TimeTab.YEAR -> {
                when {
                    summaryValue >= dailyGoalMl -> "Il tuo andamento annuale è eccellente, hai una media perfetta."
                    summaryValue > 0 -> "La tua media di quest'anno si aggira intorno a ${formatVolume(summaryValue)} al giorno."
                    else -> "Tieni traccia della tua idratazione per sbloccare le statistiche annuali."
                }
            }
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
        // RIMOSSO IL PADDING ORIZZONTALE GLOBALE! Ora la lista tocca i bordi.
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Padding aggiunto SOLO agli elementi che devono stare staccati dai bordi
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.shiftTime(false) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Precedente", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(text = dateRangeText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                IconButton(
                    onClick = { viewModel.shiftTime(true) },
                    enabled = isNextEnabled
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Successivo",
                        tint = if (isNextEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(text = "$summaryValue ml", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)

                Text(
                    text = if (currentTab == TimeTab.DAY) "Totale in questo giorno" else "Media giornaliera in questo periodo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = insightMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (currentTab != TimeTab.DAY) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
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

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Text(
                text = if (currentTab == TimeTab.DAY) "Cronologia delle bevute" else "Dettagli del periodo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (currentTab == TimeTab.DAY) {
                if (recordsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Nessun dato per questo giorno", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(recordsList, key = { it.id }) { record ->
                            val drinkData = DrinkCatalog.options.find { it.name == record.drinkName }
                            val dynamicIcon = drinkData?.icon ?: Icons.Default.LocalDrink
                            val themeBg = drinkData?.theme?.bg ?: MaterialTheme.colorScheme.surfaceVariant
                            val themeFg = drinkData?.theme?.fg ?: MaterialTheme.colorScheme.primary

                            DrinkRecordItem(
                                record = record,
                                dynamicIcon = dynamicIcon,
                                themeBg = themeBg,
                                themeFg = themeFg,
                                onEdit = { recordToEdit = record }
                            )
                        }
                    }
                }
            } else {
                if (listItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Nessun dato per questo periodo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(listItems) { item ->
                            // QUESTO È IL COMPONENTE RIASSUNTIVO UNIFICATO PER GLI ALTRI TAB
                            SummaryRecordItem(
                                title = item.title,
                                subtitle = item.subtitle,
                                valueText = "${item.value} ml",
                                isAverage = item.isAverage,
                                isSuccess = item.value >= dailyGoalMl,
                                successColor = successColor,
                                onClick = { viewModel.drillDownTo(item.timestamp, item.targetTab) }
                            )
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
                            viewModel.deleteRecord(recordToEdit!!, fitbitRepository)
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
                        viewModel.deleteRecord(recordToEdit!!, fitbitRepository)
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

// Nuovo componente per i Tab Settimana/Mese/Anno identico allo stile di DrinkRecordItem
@Composable
fun SummaryRecordItem(
    title: String,
    subtitle: String,
    valueText: String,
    isAverage: Boolean,
    isSuccess: Boolean,
    successColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle.isNotEmpty()) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = valueText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isSuccess) successColor else MaterialTheme.colorScheme.onSurface)
                if (isAverage) {
                    Text(text = "Media giorn.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 1.dp)
    }
}