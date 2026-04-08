package com.stefanorussu.hydrationtracker.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
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
import com.stefanorussu.hydrationtracker.ui.DrinkCatalog
import com.stefanorussu.hydrationtracker.ui.DrinkOption
import com.stefanorussu.hydrationtracker.ui.LocalSnackbarHostState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(waterViewModel: WaterViewModel, profileViewModel: ProfileViewModel) {
    val todayTotalRaw by waterViewModel.todayTotal.collectAsState(initial = 0)
    val todayTotal = todayTotalRaw ?: 0
    val dailyRecords by waterViewModel.dailyRecords.collectAsState(initial = emptyList())
    val profile by profileViewModel.userProfile.collectAsState()
    val dailyGoal = profileViewModel.calculateGoal(profile).takeIf { it > 0 } ?: 2000
    val drinkFrequencies by waterViewModel.drinkFrequencies.collectAsState(initial = emptyList())

    val isRefreshing by waterViewModel.isRefreshing.collectAsState()

    var selectedAmount by remember { mutableIntStateOf(250) }
    var customAmountText by remember { mutableStateOf("") }
    var recordToEdit by remember { mutableStateOf<WaterRecord?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    // CONNESSIONE ALLA SNACKBAR GLOBALE
    val globalSnackbar = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        waterViewModel.snackbarMessage.collect { message ->
            globalSnackbar.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    val extendedDrinkOptions = DrinkCatalog.options

    val sortedDrinkOptions = remember(extendedDrinkOptions, drinkFrequencies) {
        extendedDrinkOptions.sortedByDescending { drink ->
            drinkFrequencies.find { it.drinkName == drink.name }?.count ?: 0
        }
    }

    var selectedDrink by remember(sortedDrinkOptions) {
        mutableStateOf(sortedDrinkOptions.firstOrNull() ?: extendedDrinkOptions[0])
    }

    LaunchedEffect(selectedDrink) {
        waterViewModel.getSuggestedAmount(selectedDrink.name) { suggested ->
            suggested?.let {
                if (it == 200 || it == 250 || it == 500) {
                    selectedAmount = it
                    customAmountText = ""
                } else {
                    selectedAmount = 0
                    customAmountText = it.toString()
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val successColor = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF388E3C)

    val animatedTotal by animateIntAsState(
        targetValue = todayTotal,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "TotalAnimation"
    )
    val targetProgress = (todayTotal.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ProgressAnimation"
    )
    val percentage = (animatedProgress * 100).toInt()

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            waterViewModel.syncWithFitbit(isManual = true)
            delay(500)
            pullToRefreshState.endRefresh()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                waterViewModel.syncWithFitbit(isManual = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Hydration Tracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            )
        }
        // RIMOSSO: Lo SnackbarHost locale non serve più!
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(bottom = 8.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Progresso di oggi",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$animatedTotal / $dailyGoal ml",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (percentage >= 100) successColor else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                        strokeCap = StrokeCap.Round
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "$percentage%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (percentage >= 100) successColor else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Text(
                        text = "1. Scegli la Bevanda",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                item {
                    val displayDrinks = sortedDrinkOptions.take(4)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        displayDrinks.forEach { drink ->
                            val isSelected = selectedDrink == drink
                            val scaleAnim by animateFloatAsState(
                                targetValue = if (isSelected) 1.1f else 1f,
                                label = "Scale"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedDrink = drink }
                                    .scale(scaleAnim)
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(drink.theme.bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = drink.icon,
                                        contentDescription = drink.name,
                                        tint = drink.theme.fg,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = drink.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { showBottomSheet = true }
                                .weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Altro",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Altro",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Text(
                        text = "2. Scegli la Quantità",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(200, 250, 500).forEach { amount ->
                            Surface(
                                selected = (selectedAmount == amount && customAmountText.isEmpty()),
                                onClick = {
                                    selectedAmount = amount
                                    customAmountText = ""
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier
                                    .height(44.dp)
                                    .weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedAmount == amount && customAmountText.isEmpty()) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                border = if (selectedAmount == amount && customAmountText.isEmpty()) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${amount}ml",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        val isCustomSelected = customAmountText.isNotEmpty() || (selectedAmount != 200 && selectedAmount != 250 && selectedAmount != 500)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCustomSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            border = if (isCustomSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .height(44.dp)
                                .weight(1f)
                        ) {
                            BasicTextField(
                                value = customAmountText,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) {
                                        customAmountText = it
                                        if (it.isNotEmpty()) selectedAmount = 0
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .wrapContentHeight(Alignment.CenterVertically)
                                    .onFocusChanged { if (it.isFocused) selectedAmount = 0 },
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (customAmountText.isEmpty()) {
                                            Text(
                                                text = "ml",
                                                style = TextStyle(
                                                    textAlign = TextAlign.Center,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    val inputAmount = customAmountText.toIntOrNull() ?: selectedAmount
                    Button(
                        onClick = {
                            if (inputAmount > 0) {
                                val actualHydration = (inputAmount * selectedDrink.hydrationFactor).toInt()
                                waterViewModel.addWater(actualHydration, inputAmount, selectedDrink.name)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                customAmountText = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (inputAmount > 0) "AGGIUNGI $inputAmount ML DI ${selectedDrink.name.uppercase(Locale.getDefault())}" else "INSERISCI QUANTITÀ",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "Cronologia",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                    )
                }

                items(dailyRecords) { record ->
                    val drinkData = extendedDrinkOptions.find { it.name == record.drinkName }
                    val dynamicIcon = drinkData?.icon ?: Icons.Default.LocalDrink
                    val themeBg = drinkData?.theme?.bg ?: MaterialTheme.colorScheme.surfaceVariant
                    val themeFg = drinkData?.theme?.fg ?: MaterialTheme.colorScheme.primary

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { recordToEdit = record }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(themeBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = dynamicIcon,
                                contentDescription = null,
                                tint = themeFg,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = record.drinkName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tocca per modificare",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+${record.amountMl} ml",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = timeFormatter.format(Date(record.timestamp)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
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

                val categories = listOf(
                    "Idratazione Pura" to listOf(extendedDrinkOptions.find { it.name == "Acqua" }!!, extendedDrinkOptions.find { it.name == "Tisana" }!!, extendedDrinkOptions.find { it.name == "Tè" }!!, extendedDrinkOptions.find { it.name == "Succo" }!!),
                    "Energia & Fitness" to listOf(extendedDrinkOptions.find { it.name == "Caffè" }!!, extendedDrinkOptions.find { it.name == "Sport Drink" }!!, extendedDrinkOptions.find { it.name == "Proteine" }!!, extendedDrinkOptions.find { it.name == "Bibita" }!!),
                    "Nutrizione & Pasti" to listOf(extendedDrinkOptions.find { it.name == "Latte" }!!, extendedDrinkOptions.find { it.name == "Cioccolata" }!!, extendedDrinkOptions.find { it.name == "Yogurt" }!!, extendedDrinkOptions.find { it.name == "Brodo" }!!),
                    "Alcolici" to listOf(extendedDrinkOptions.find { it.name == "Birra" }!!, extendedDrinkOptions.find { it.name == "Vino" }!!, extendedDrinkOptions.find { it.name == "Cocktail" }!!)
                )

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    categories.forEach { (categoryName, drinks) ->
                        Column {
                            Text(
                                text = categoryName.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            title = {
                Text(
                    text = "Modifica ${recordToEdit!!.drinkName}",
                    fontWeight = FontWeight.Bold
                )
            },
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

                    Text(
                        text = "Data e Ora",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                            waterViewModel.updateRecord(recordToEdit!!, newVal, editTimestamp)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else {
                            waterViewModel.deleteWater(recordToEdit!!)
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
                        waterViewModel.deleteWater(recordToEdit!!)
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

@Composable
fun DrinkGridItem(drink: DrinkOption, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = drink.theme.bg
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = drink.icon,
                    contentDescription = drink.name,
                    tint = drink.theme.fg,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = drink.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}