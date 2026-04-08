package com.stefanorussu.hydrationtracker.ui // Modifica il pacchetto se necessario

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// STRUTTURA DEI COLORI STILE PIXEL
data class DrinkTheme(val bg: Color, val fg: Color)
val themeBlue = DrinkTheme(Color(0xFFD3E3FD), Color(0xFF041E49))   // Idratazione
val themeOrange = DrinkTheme(Color(0xFFFFDCC1), Color(0xFF2E1500)) // Energia
val themeGreen = DrinkTheme(Color(0xFFC4EED0), Color(0xFF072711))  // Nutrizione
val themePurple = DrinkTheme(Color(0xFFEADDFF), Color(0xFF21005D)) // Alcolici

data class DrinkOption(val name: String, val icon: ImageVector, val hydrationFactor: Float, val theme: DrinkTheme)

object DrinkCatalog {
    val options = listOf(
        DrinkOption("Acqua", Icons.Default.LocalDrink, 1.0f, themeBlue),
        DrinkOption("Tisana", Icons.Default.Eco, 1.0f, themeBlue),
        DrinkOption("Tè", Icons.Default.EmojiFoodBeverage, 0.99f, themeBlue),
        DrinkOption("Succo", Icons.Default.LocalFlorist, 0.85f, themeBlue),
        DrinkOption("Caffè", Icons.Default.Coffee, 0.98f, themeOrange),
        DrinkOption("Sport Drink", Icons.Default.FitnessCenter, 0.90f, themeOrange),
        DrinkOption("Bibita", Icons.Default.Fastfood, 0.89f, themeOrange),
        DrinkOption("Proteine", Icons.Default.Bolt, 0.85f, themeOrange),
        DrinkOption("Latte", Icons.Default.LocalCafe, 0.88f, themeGreen),
        DrinkOption("Brodo", Icons.Default.Restaurant, 0.95f, themeGreen),
        DrinkOption("Cioccolata", Icons.Default.FreeBreakfast, 0.85f, themeGreen),
        DrinkOption("Yogurt", Icons.Default.Icecream, 0.80f, themeGreen),
        DrinkOption("Birra", Icons.Default.SportsBar, 0.80f, themePurple),
        DrinkOption("Vino", Icons.Default.WineBar, 0.50f, themePurple),
        DrinkOption("Cocktail", Icons.Default.LocalBar, 0.40f, themePurple)
    )
}