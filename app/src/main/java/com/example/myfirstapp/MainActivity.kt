package com.example.myfirstapp

import WaterRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.myfirstapp.data.local.AppDatabase
import com.example.myfirstapp.data.repository.WaterRepository
import com.example.myfirstapp.ui.screens.HomeScreen
import com.example.myfirstapp.ui.theme.MyFirstAppTheme
import com.example.myfirstapp.ui.viewmodel.WaterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inizializziamo il Database Room
        // "water_db" è il nome del file fisico sul telefono
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "water_db"
        ).fallbackToDestructiveMigration() // Utile in fase di sviluppo se modifichi le tabelle
            .build()

        // 2. Creiamo il Repository passando il DAO
        val repository = WaterRepository(db.waterDao())

        // 3. Creiamo il ViewModel passando il Repository
        val viewModel = WaterViewModel(repository)

        setContent {
            // Utilizziamo il tema generato automaticamente da Android Studio
            MyFirstAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 4. Avviamo la schermata principale
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}