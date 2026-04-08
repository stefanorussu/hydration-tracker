package com.stefanorussu.hydrationtracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: WaterRecord): Long

    @Query("UPDATE water_logs SET externalId = :fitbitId, source = 'LOCAL' WHERE id = :recordId")
    suspend fun updateFitbitId(recordId: Int, fitbitId: String)

    @Update
    suspend fun updateWater(record: WaterRecord)

    @Delete
    suspend fun deleteWater(record: WaterRecord)

    @Query("SELECT SUM(amountMl) FROM water_logs WHERE timestamp BETWEEN :startOfDay AND :endOfDay")
    fun getTodayTotal(startOfDay: Long, endOfDay: Long): Flow<Int?>

    @Query("SELECT * FROM water_logs WHERE timestamp BETWEEN :startOfDay AND :endOfDay ORDER BY timestamp DESC")
    fun getRecordsBetweenDates(startOfDay: Long, endOfDay: Long): Flow<List<WaterRecord>>

    @Query("SELECT drinkName, COUNT(id) as count FROM water_logs GROUP BY drinkName ORDER BY count DESC")
    fun getDrinkFrequencies(): Flow<List<DrinkFrequency>>

    // QUESTA L'AVEVO CANCELLATA PER SBAGLIO (Serve al Backup!)
    @Query("SELECT * FROM water_logs ORDER BY timestamp ASC")
    suspend fun getAllRecords(): List<WaterRecord>

    // Funzione Statistiche aggiornata con dateMillis
    @Query("SELECT MAX(timestamp) as dateMillis, SUM(amountMl) as totalMl FROM water_logs WHERE timestamp BETWEEN :startOfDay AND :endOfDay GROUP BY date(timestamp/1000, 'unixepoch') ORDER BY dateMillis ASC")
    fun getStatsBetweenDates(startOfDay: Long, endOfDay: Long): Flow<List<DailyWaterStats>>

    // Controlla se esiste già un record con un determinato ID di Fitbit
    @Query("SELECT COUNT(*) FROM water_logs WHERE externalId = :fitbitId")
    suspend fun checkFitbitIdCount(fitbitId: String): Int

    // Recupera tutti i record di oggi che sono collegati a Fitbit
    @Query("SELECT * FROM water_logs WHERE timestamp BETWEEN :startOfDay AND :endOfDay AND externalId IS NOT NULL")
    suspend fun getSyncedRecordsForToday(startOfDay: Long, endOfDay: Long): List<WaterRecord>

    // Ora cerca la quantità "fisica" più usata (inputAmountMl)
    @Query("SELECT inputAmountMl FROM water_logs WHERE drinkName = :drinkName GROUP BY inputAmountMl ORDER BY COUNT(inputAmountMl) DESC LIMIT 1")
    suspend fun getMostFrequentAmount(drinkName: String): Int?
}