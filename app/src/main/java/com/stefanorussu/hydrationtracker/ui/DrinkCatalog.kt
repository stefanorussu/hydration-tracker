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
        // 1. Idratazione Pura (Tonalità Azzurre e Verdi tenui)
        DrinkOption("Acqua", 1.0f, Icons.Default.WaterDrop, DrinkTheme(Color(0xFFD6E4FF), Color(0xFF00468A))),
        DrinkOption("Acq. Frizzante", 1.0f, Icons.Default.WaterDrop, DrinkTheme(Color(0xFFC7F0FF), Color(0xFF004E59))),
        DrinkOption("Tè", 1.0f, Icons.Default.EmojiFoodBeverage, DrinkTheme(Color(0xFFD7E8CD), Color(0xFF1B4B14))),
        DrinkOption("Tisana", 1.0f, Icons.Default.EmojiFoodBeverage, DrinkTheme(Color(0xFFE2F0CB), Color(0xFF2E6B22))),
        DrinkOption("Limonata", 0.9f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFFFF0C2), Color(0xFF5B4300))),

        // 2. Caffetteria (Tonalità Calde e Terra)
        DrinkOption("Caffè", 0.8f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFEAE0D5), Color(0xFF4A392F))),
        DrinkOption("Decaffeinato", 1.0f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFF3EAE1), Color(0xFF5D4A3D))),
        DrinkOption("Cappuccino", 0.9f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFFFDBCF), Color(0xFF6C2700))),
        DrinkOption("Cioccolata", 0.85f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFEEDACC), Color(0xFF4D3324))),

        // 3. Latte e Derivati (Tonalità Bianche/Violacee)
        DrinkOption("Latte", 0.95f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFE2E2E9), Color(0xFF303036))),
        DrinkOption("Latte Veg.", 0.95f, Icons.Default.LocalCafe, DrinkTheme(Color(0xFFF0EBE1), Color(0xFF4E453A))),
        DrinkOption("Yogurt", 0.85f, Icons.Default.LocalDining, DrinkTheme(Color(0xFFEBD4FF), Color(0xFF4D1F7B))),

        // 4. Frutta e Vitamine (Tonalità Arancio/Rosate)
        DrinkOption("Spremuta", 0.9f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFFFDCC1), Color(0xFF6D3000))),
        DrinkOption("Succo", 0.8f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFFFDEAD), Color(0xFF663B00))),
        DrinkOption("Frullato", 0.8f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFFFD8E4), Color(0xFF631133))),
        DrinkOption("Acq. Cocco", 1.05f, Icons.Default.LocalDrink, DrinkTheme(Color(0xFFE8F5E9), Color(0xFF1B5E20))),

        // 5. Energia & Fitness (Tonalità Vivaci ma Morbide)
        DrinkOption("Sport Drink", 1.0f, Icons.Default.FlashOn, DrinkTheme(Color(0xFFCFF3FC), Color(0xFF004E59))),
        DrinkOption("Energy Drink", 0.8f, Icons.Default.Bolt, DrinkTheme(Color(0xFFFFE082), Color(0xFF5C3F00))),

        // 6. Alcolici e Relax (Tonalità Serali)
        DrinkOption("Birra", 0.4f, Icons.Default.SportsBar, DrinkTheme(Color(0xFFFFDF94), Color(0xFF5C3F00))),
        DrinkOption("Vino", 0.3f, Icons.Default.WineBar, DrinkTheme(Color(0xFFFFDAD6), Color(0xFF730005))),
        DrinkOption("Cocktail", 0.25f, Icons.Default.LocalBar, DrinkTheme(Color(0xFFE0E0FF), Color(0xFF282470))),
        DrinkOption("Digestivo", 0.3f, Icons.Default.LocalBar, DrinkTheme(Color(0xFFC9EBE1), Color(0xFF005141))),
        DrinkOption("Superalcolico", 0.2f, Icons.Default.LocalBar, DrinkTheme(Color(0xFFFFCDD2), Color(0xFFB71C1C)))
    )
}