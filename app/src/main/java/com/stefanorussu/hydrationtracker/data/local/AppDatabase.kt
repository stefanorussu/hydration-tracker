package com.stefanorussu.hydrationtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ... importazioni ...

@Database(entities = [WaterRecord::class], version = 2, exportSchema = false) // Mettiamo version = 2
abstract class AppDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hydration_database"
                )
                    .fallbackToDestructiveMigration() // DISTRUGGE E RICREA IN CASO DI MODIFICHE
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}