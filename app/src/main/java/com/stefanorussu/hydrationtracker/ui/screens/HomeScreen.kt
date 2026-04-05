package com.stefanorussu.hydrationtracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stefanorussu.hydrationtracker.model.DrinkType
import com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel
import com.stefanorussu.hydrationtracker.ui.viewmodel.WaterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    waterViewModel: WaterViewModel,
    profileViewModel: ProfileViewModel,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val totalWater by waterViewModel.todayTotal.collectAsState()
    val profile by profileViewModel.userProfile.collectAsState()
    val logs by waterViewModel.logs.collectAsState()

    // --- COLORI DINAMICI ---
    val DeepNavy = MaterialTheme.colorScheme.onBackground
    val SoftSlate = MaterialTheme.colorScheme.onSurfaceVariant
    val ModernBlue = MaterialTheme.colorScheme.primary
    val SuccessGreen = Color(0xFF006D39)
    val LightSurface = MaterialTheme.colorScheme.background

    // --- STATO ---
    val amounts = listOf(100, 250, 330, 500)
    var selectedAmount by remember { mutableStateOf(0) }
    var customAmountText by remember { mutableStateOf("") }

    // Calcolo dinamico basato sul profilo salvato
    val dailyGoal = profileViewModel.calculateGoal(profile)
    val percentage = if (dailyGoal > 0) (totalWater * 100 / dailyGoal) else 0

    val sortedDrinkTypes = remember(logs) {
        DrinkType.entries.toTypedArray().sortedByDescending { type ->
            logs.count { it.drinkType == type.name }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (totalWater.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f),
        label = "progress"
    )
    val percentageColor by animateColorAsState(
        targetValue = if (percentage >= 100) SuccessGreen else ModernBlue,
        label = "color"
    )

    Scaffold(
        containerColor = LightSurface,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "H2O",
                        style = MaterialTheme.typography.labelLarge,
                        color = SoftSlate,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Black
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                actions = {
                    // TASTO PROFILO
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = "Profilo", tint = SoftSlate)
                    }
                    // TASTO IMPOSTAZIONI
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni", tint = SoftSlate)
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
            Spacer(modifier = Modifier.height(12.dp))

            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(text = "$totalWater", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, fontSize = 80.sp, letterSpacing = (-4).sp), color = DeepNavy)
                    Text(text = "ml di $dailyGoal obiettivo", style = MaterialTheme.typography.titleMedium, color = SoftSlate)
                }
                Text(text = "$percentage%", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp), color = percentageColor, modifier = Modifier.padding(bottom = 12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                strokeCap = StrokeCap.Round,
                color = percentageColor,
                trackColor = Color(0xFFE1E2E4)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- 1. SELEZIONE QUANTITÀ ---
            Text(text = "1. QUANTO HAI BEVUTO?", style = MaterialTheme.typography.labelSmall, color = SoftSlate)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                amounts.forEach { amount ->
                    FilterChip(
                        selected = selectedAmount == amount && customAmountText.isEmpty(),
                        onClick = {
                            selectedAmount = amount
                            customAmountText = ""
                        },
                        label = { Text("$amount") },
                        shape = CircleShape
                    )
                }

                val isCustomActive = customAmountText.isNotEmpty()
                Surface(
                    modifier = Modifier.width(80.dp).height(32.dp),
                    shape = CircleShape,
                    color = if (isCustomActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCustomActive) ModernBlue else MaterialTheme.colorScheme.outline)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BasicTextField(
                            value = customAmountText,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() } && it.length <= 4) {
                                    customAmountText = it
                                    selectedAmount = it.toIntOrNull() ?: 0
                                }
                            },
                            textStyle = TextStyle(textAlign = TextAlign.Center, color = DeepNavy, fontSize = 14.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = SolidColor(ModernBlue),
                            decorationBox = { innerTextField ->
                                if (customAmountText.isEmpty()) Text("Altro", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = TextStyle(color = SoftSlate, fontSize = 14.sp))
                                innerTextField()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. SELEZIONE BEVANDA ---
            Text(text = "2. COSA HAI BEVUTO?", style = MaterialTheme.typography.labelSmall, color = SoftSlate)
            LazyRow(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(sortedDrinkTypes) { type ->
                    val isEnabled = selectedAmount > 0
                    val itemShape = RoundedCornerShape(12.dp)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(70.dp)
                            .clip(itemShape)
                            .background(
                                color = if (isEnabled) Color(0xFFE9EEF4) else Color.Transparent,
                                shape = itemShape
                            )
                            .clickable(enabled = isEnabled) {
                                waterViewModel.addDrink(selectedAmount, type) // Corretto: waterViewModel
                                selectedAmount = 0
                                customAmountText = ""
                            }
                            .padding(vertical = 10.dp)
                            .graphicsLayer(alpha = if (isEnabled) 1f else 0.3f)
                    ) {
                        Text(text = type.icon, fontSize = 26.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                            color = if (isEnabled) DeepNavy else SoftSlate,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. CRONOLOGIA ---
            Text(text = "ULTIME BEVANDE", style = MaterialTheme.typography.labelSmall, color = SoftSlate)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs) { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE9EEF4)), contentAlignment = Alignment.Center) {
                            val icon = try { DrinkType.valueOf(log.drinkType).icon } catch(e: Exception) { "💧" }
                            Text(icon, fontSize = 20.sp)
                        }
                        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                            Text("${log.amountMl} ml", fontWeight = FontWeight.Bold, color = DeepNavy)
                            Text(log.drinkType, style = MaterialTheme.typography.bodySmall, color = SoftSlate)
                        }
                        IconButton(onClick = { waterViewModel.removeLog(log) }) { // Corretto: waterViewModel
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFC4C7C5), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}