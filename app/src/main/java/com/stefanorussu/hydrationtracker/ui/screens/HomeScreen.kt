package com.stefanorussu.hydrationtracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.WaterViewModel
import java.text.SimpleDateFormat
import java.util.*

data class DrinkOption(val name: String, val icon: ImageVector, val hydrationFactor: Float)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(waterViewModel: WaterViewModel, profileViewModel: ProfileViewModel) {
    val todayTotalRaw by waterViewModel.todayTotal.collectAsState(initial = 0)
    val todayTotal = todayTotalRaw ?: 0
    val dailyRecords by waterViewModel.dailyRecords.collectAsState(initial = emptyList())
    val profile by profileViewModel.userProfile.collectAsState()
    val dailyGoal = profileViewModel.calculateGoal(profile).takeIf { it > 0 } ?: 2000
    val drinkFrequencies by waterViewModel.drinkFrequencies.collectAsState(initial = emptyList())

    var selectedAmount by remember { mutableIntStateOf(250) }
    var customAmountText by remember { mutableStateOf("") }
    var recordToEdit by remember { mutableStateOf<WaterRecord?>(null) }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    // CATALAGO BEVANDE COMPLETO E OTTIMIZZATO
    val extendedDrinkOptions = listOf(
        DrinkOption("Acqua", Icons.Default.LocalDrink, 1.0f),
        DrinkOption("Tisana", Icons.Default.Eco, 1.0f),
        DrinkOption("Tè", Icons.Default.EmojiFoodBeverage, 0.99f),
        DrinkOption("Caffè", Icons.Default.Coffee, 0.98f),
        DrinkOption("Brodo", Icons.Default.Restaurant, 0.95f),
        DrinkOption("Sport Drink", Icons.Default.FitnessCenter, 0.90f),
        DrinkOption("Bibita", Icons.Default.Fastfood, 0.89f),
        DrinkOption("Latte", Icons.Default.LocalCafe, 0.88f),
        DrinkOption("Proteine", Icons.Default.Bolt, 0.85f),
        DrinkOption("Succo", Icons.Default.LocalFlorist, 0.85f),
        DrinkOption("Cioccolata", Icons.Default.FreeBreakfast, 0.85f),
        DrinkOption("Yogurt", Icons.Default.Icecream, 0.80f),
        DrinkOption("Birra", Icons.Default.SportsBar, 0.80f),
        DrinkOption("Vino", Icons.Default.WineBar, 0.50f),
        DrinkOption("Cocktail", Icons.Default.LocalBar, 0.40f)
    )

    val sortedDrinkOptions = remember(extendedDrinkOptions, drinkFrequencies) {
        extendedDrinkOptions.sortedByDescending { drink -> drinkFrequencies.find { it.drinkName == drink.name }?.count ?: 0 }
    }

    val topDrinks = sortedDrinkOptions.take(5)
    var selectedDrink by remember(sortedDrinkOptions) { mutableStateOf(sortedDrinkOptions.firstOrNull() ?: extendedDrinkOptions[0]) }

    val focusManager = LocalFocusManager.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val successColor = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF388E3C)

    val animatedTotal by animateIntAsState(targetValue = todayTotal, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing), label = "TotalAnimation")
    val targetProgress = (todayTotal.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing), label = "ProgressAnimation")
    val percentage = (animatedProgress * 100).toInt()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Hydration Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = "Progresso di oggi", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "$animatedTotal / $dailyGoal ml", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)), color = if (percentage >= 100) successColor else MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), strokeCap = StrokeCap.Round)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "$percentage%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (percentage >= 100) successColor else MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Quantità", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(100, 250, 500).forEach { amount ->
                    FilterChip(selected = (selectedAmount == amount && customAmountText.isEmpty()), onClick = { selectedAmount = amount; customAmountText = ""; focusManager.clearFocus() }, label = { Text("${amount}ml", style = MaterialTheme.typography.bodyMedium) }, shape = RoundedCornerShape(8.dp))
                }
                OutlinedTextField(value = customAmountText, onValueChange = { if (it.all { c -> c.isDigit() }) { customAmountText = it; if (it.isNotEmpty()) selectedAmount = 0 } }, placeholder = { Text("ml", style = MaterialTheme.typography.bodyMedium) }, modifier = Modifier.weight(1f).height(50.dp).onFocusChanged { if (it.isFocused) selectedAmount = 0 }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(8.dp), textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface), singleLine = true)
            }

            Text(text = "Bevanda", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(topDrinks) { drink ->
                    val isSelected = selectedDrink == drink
                    val scaleAnim by animateFloatAsState(if (isSelected) 1.15f else 0.95f, label = "Scale")
                    val alphaAnim by animateFloatAsState(if (isSelected) 1f else 0.6f, label = "Alpha")

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { selectedDrink = drink }.scale(scaleAnim).alpha(alphaAnim).padding(vertical = 8.dp)) {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Icon(imageVector = drink.icon, contentDescription = drink.name, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = drink.name, style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showBottomSheet = true }.padding(vertical = 8.dp)) {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Altro", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Altro...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            val inputAmount = customAmountText.toIntOrNull() ?: selectedAmount
            Button(
                onClick = {
                    if (inputAmount > 0) {
                        val actualHydration = (inputAmount * selectedDrink.hydrationFactor).toInt()
                        waterViewModel.addWater(actualHydration, selectedDrink.name)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        customAmountText = ""; focusManager.clearFocus()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = if (inputAmount > 0) "AGGIUNGI $inputAmount ML DI ${selectedDrink.name.uppercase(Locale.getDefault())}" else "INSERISCI QUANTITÀ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            Text(text = "Cronologia", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 4.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(dailyRecords) { record ->
                    val dynamicIcon = extendedDrinkOptions.find { it.name == record.drinkName }?.icon ?: Icons.Default.LocalDrink
                    Row(modifier = Modifier.fillMaxWidth().clickable { recordToEdit = record }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Icon(imageVector = dynamicIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = record.drinkName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "Tocca per modificare", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "+${record.amountMl} ml", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = timeFormatter.format(Date(record.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Cosa hai bevuto?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // CATEGORIZZAZIONE INTELLIGENTE
                val categories = listOf(
                    "Idratazione Pura" to listOf(
                        extendedDrinkOptions.find { it.name == "Acqua" }!!,
                        extendedDrinkOptions.find { it.name == "Tisana" }!!,
                        extendedDrinkOptions.find { it.name == "Tè" }!!,
                        extendedDrinkOptions.find { it.name == "Succo" }!!
                    ),
                    "Energia & Fitness" to listOf(
                        extendedDrinkOptions.find { it.name == "Caffè" }!!,
                        extendedDrinkOptions.find { it.name == "Sport Drink" }!!,
                        extendedDrinkOptions.find { it.name == "Proteine" }!!,
                        extendedDrinkOptions.find { it.name == "Bibita" }!!
                    ),
                    "Nutrizione & Pasti" to listOf(
                        extendedDrinkOptions.find { it.name == "Latte" }!!,
                        extendedDrinkOptions.find { it.name == "Cioccolata" }!!,
                        extendedDrinkOptions.find { it.name == "Yogurt" }!!,
                        extendedDrinkOptions.find { it.name == "Brodo" }!!
                    ),
                    "Alcolici" to listOf(
                        extendedDrinkOptions.find { it.name == "Birra" }!!,
                        extendedDrinkOptions.find { it.name == "Vino" }!!,
                        extendedDrinkOptions.find { it.name == "Cocktail" }!!
                    )
                )

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    categories.forEach { (categoryName, drinks) ->
                        Column {
                            Text(
                                text = categoryName.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                drinks.forEach { drink ->
                                    DrinkGridItem(
                                        drink = drink,
                                        onClick = {
                                            selectedDrink = drink
                                            showBottomSheet = false
                                        }
                                    )
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
                    OutlinedTextField(value = editAmountText, onValueChange = { if (it.all { c -> c.isDigit() }) editAmountText = it }, label = { Text("Quantità (ml)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Data e Ora", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { val c = Calendar.getInstance().apply { timeInMillis = editTimestamp }; android.app.DatePickerDialog(context, { _, year, month, dayOfMonth -> val newC = Calendar.getInstance().apply { timeInMillis = editTimestamp }; newC.set(year, month, dayOfMonth); editTimestamp = newC.timeInMillis }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(dialogDateFormatter.format(Date(editTimestamp))) }
                        OutlinedButton(onClick = { val c = Calendar.getInstance().apply { timeInMillis = editTimestamp }; android.app.TimePickerDialog(context, { _, hourOfDay, minute -> val newC = Calendar.getInstance().apply { timeInMillis = editTimestamp }; newC.set(Calendar.HOUR_OF_DAY, hourOfDay); newC.set(Calendar.MINUTE, minute); editTimestamp = newC.timeInMillis }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(dialogTimeFormatter.format(Date(editTimestamp))) }
                    }
                }
            },
            confirmButton = { Button(onClick = { val newVal = editAmountText.toIntOrNull() ?: 0; if (newVal > 0) { waterViewModel.updateRecord(recordToEdit!!, newVal, editTimestamp); hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) } else { waterViewModel.deleteWater(recordToEdit!!) }; recordToEdit = null }) { Text("Salva") } },
            dismissButton = { TextButton(onClick = { waterViewModel.deleteWater(recordToEdit!!); recordToEdit = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Elimina") } }
        )
    }
}

@Composable
fun DrinkGridItem(drink: DrinkOption, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = drink.icon,
                    contentDescription = drink.name,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = drink.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}