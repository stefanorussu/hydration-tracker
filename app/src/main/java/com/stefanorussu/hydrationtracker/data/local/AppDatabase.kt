package com.stefanorussu.hydrationtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.stefanorussu.hydrationtracker.data.local.entities.WaterLog

@Database(entities = [WaterLog::class], version = 1, exportSchema = false)
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
                    "water_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}