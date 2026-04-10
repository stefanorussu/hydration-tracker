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
        // Idratazione Pura (Tonalità Azzurre e Verdi tenui)
        DrinkOption("Acqua", 1.0f, Icons.Default.WaterDrop, DrinkTheme(Color(0xFFD6E4FF), Color(0xFF00468A))),
        DrinkOption("Tè", 1.0f, Icons.Default.EmojiFoodBeverage, DrinkTheme(Color(0xFFD7E8CD), Color(0xFF1B4B14))),
        DrinkOption("Limonata", 0.9f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFFFF0C2), Color(0xFF5B4300))),

        // Caffetteria (Tonalità Calde e Terra)
        DrinkOption("Caffè", 0.8f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFEAE0D5), Color(0xFF4A392F))),
        DrinkOption("Cappuccino", 0.9f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFFFDBCF), Color(0xFF6C2700))),
        DrinkOption("Cioccolata", 0.8f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFEEDACC), Color(0xFF4D3324))),

        // Energia & Fitness (Tonalità Vivaci ma Morbide)
        DrinkOption("Sport Drink", 1.0f, Icons.Default.FlashOn, DrinkTheme(Color(0xFFCFF3FC), Color(0xFF004E59))),
        DrinkOption("Energy", 0.7f, Icons.Default.Bolt, DrinkTheme(Color(0xFFFFDCC1), Color(0xFF6D3000))),
        DrinkOption("Frullato", 0.9f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFFFD8E4), Color(0xFF631133))),

        // Pasti & Nutrizione (Tonalità Neutre e Violacee)
        DrinkOption("Latte", 1.0f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFE2E2E9), Color(0xFF303036))),
        DrinkOption("Yogurt", 0.8f, Icons.Default.LocalDining, DrinkTheme(Color(0xFFEBD4FF), Color(0xFF4D1F7B))),
        DrinkOption("Succo", 0.9f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFFFDEAD), Color(0xFF663B00))),

        // Alcolici e Relax (Tonalità Serali)
        DrinkOption("Birra", 0.4f, Icons.Default.SportsBar, DrinkTheme(Color(0xFFFFDF94), Color(0xFF5C3F00))),
        DrinkOption("Vino", -0.2f, Icons.Default.WineBar, DrinkTheme(Color(0xFFFFDAD6), Color(0xFF730005))),
        DrinkOption("Cocktail", -0.5f, Icons.Default.LocalBar, DrinkTheme(Color(0xFFE0E0FF), Color(0xFF282470))),
        DrinkOption("Digestivo", -0.8f, Icons.Default.LocalBar, DrinkTheme(Color(0xFFC9EBE1), Color(0xFF005141)))
    )
}