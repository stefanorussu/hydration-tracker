package com.stefanorussu.hydrationtracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WaterRecord)

    @Update
    suspend fun update(record: WaterRecord)

    // Modificata per accettare il calcolo esatto della mezzanotte locale
    @Query("SELECT SUM(amountMl) FROM water_logs WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    fun getTodayTotal(startTimestamp: Long, endTimestamp: Long): Flow<Int?>

    // Rimane intatta per non rompere il backup
    @Query("""
        DELETE FROM water_logs 
        WHERE timestamp >= strftime('%s', 'now', 'start of day') * 1000
    """)
    suspend fun deleteToday()

    @Delete
    suspend fun delete(record: WaterRecord)

    @Query("SELECT * FROM water_logs")
    suspend fun getAllRecords(): List<WaterRecord>

    // MODIFICA CRONOLOGICA: Ora ordina in ASC (dal primo all'ultimo)
    @Query("""
        SELECT * FROM water_logs 
        WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp
        ORDER BY timestamp ASC
    """)
    fun getRecordsBetweenDates(startTimestamp: Long, endTimestamp: Long): Flow<List<WaterRecord>>

    @Query("""
        SELECT ((timestamp + :timezoneOffset) / 86400000) * 86400000 - :timezoneOffset AS dateMillis, 
               SUM(amountMl) AS totalMl 
        FROM water_logs 
        WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp
        GROUP BY dateMillis 
        ORDER BY dateMillis ASC
    """)
    fun getStatsBetweenDates(startTimestamp: Long, endTimestamp: Long, timezoneOffset: Long): Flow<List<DailyWaterStats>>

    @Query("SELECT drinkName, COUNT(id) as count FROM water_logs GROUP BY drinkName ORDER BY count DESC")
    fun getDrinkFrequencies(): Flow<List<DrinkFrequency>>
}

data class DailyWaterStats(
    val dateMillis: Long,
    val totalMl: Int
)

data class DrinkFrequency(
    val drinkName: String,
    val count: Int
)