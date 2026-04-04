package com.example.myfirstapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.myfirstapp.data.local.entities.WaterLog

// Qui definiamo le tabelle (entities) e la versione del database
@Database(entities = [WaterLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Questo metodo permette all'app di accedere alle funzioni di scrittura/lettura
    abstract fun waterDao(): WaterDao
}