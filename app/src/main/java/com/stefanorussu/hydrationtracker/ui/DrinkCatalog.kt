package com.stefanorussu.hydrationtracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class DrinkTheme(val bg: Color, val fg: Color)

data class DrinkOption(
    val name: String,
    val hydrationFactor: Float,
    val icon: ImageVector,
    val theme: DrinkTheme
)

object DrinkCatalog {
    val options = listOf(
        // Idratazione Pura
        DrinkOption("Acqua", 1.0f, Icons.Default.WaterDrop, DrinkTheme(Color(0xFFE3F2FD), Color(0xFF1976D2))),
        DrinkOption("Tè", 1.0f, Icons.Default.EmojiFoodBeverage, DrinkTheme(Color(0xFFF1F8E9), Color(0xFF388E3C))),
        DrinkOption("Limonata", 1.0f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFFFFDE7), Color(0xFFFBC02D))),

        // Caffetteria
        DrinkOption("Caffè", -0.2f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFEFEBE9), Color(0xFF5D4037))),
        DrinkOption("Cappuccino", 0.5f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFFBE9E7), Color(0xFFD84315))),
        DrinkOption("Cioccolata", 0.5f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFEFEBE9), Color(0xFF4E342E))), // Spostata qui

        // Energia & Fitness
        DrinkOption("Sport Drink", 1.0f, Icons.Default.FlashOn, DrinkTheme(Color(0xFFE0F7FA), Color(0xFF0097A7))), // Icona Fulmine
        DrinkOption("Energy", 0.5f, Icons.Default.Bolt, DrinkTheme(Color(0xFFFFF3E0), Color(0xFFE65100))),
        DrinkOption("Frullato", 0.8f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFFCE4EC), Color(0xFFC2185B))), // Icona bicchiere standard, pulita

        // Pasti & Nutrizione
        DrinkOption("Latte", 0.9f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFFAFAFA), Color(0xFF616161))),
        DrinkOption("Yogurt", 0.6f, Icons.Default.LocalDining, DrinkTheme(Color(0xFFF3E5F5), Color(0xFF7B1FA2))), // Icona Ristorazione (Posate/Pasto)
        DrinkOption("Succo", 0.9f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFFFF8E1), Color(0xFFFFA000))),

        // Alcolici e Relax
        DrinkOption("Birra", -0.5f, Icons.Default.SportsBar, DrinkTheme(Color(0xFFFFF8E1), Color(0xFFF57F17))),
        DrinkOption("Vino", -0.8f, Icons.Default.WineBar, DrinkTheme(Color(0xFFFFEBEE), Color(0xFFD32F2F))), // Icona Calice di Vino
        DrinkOption("Cocktail", -1.0f, Icons.Default.LocalBar, DrinkTheme(Color(0xFFE8EAF6), Color(0xFF303F9F))),
        DrinkOption("Digestivo", -1.5f, Icons.Default.LocalBar, DrinkTheme(Color(0xFFE0F2F1), Color(0xFF00796B))) // Icona Bicchiere da Cocktail/Cicchetto
    )
}