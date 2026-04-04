package com.example.myfirstapp.data.local

import androidx.room.*
import com.example.myfirstapp.data.local.entities.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLog)

    @Delete
    suspend fun deleteLog(log: WaterLog)

    @Query("SELECT SUM(waterContentMl) FROM water_logs WHERE timestamp >= :startOfDay")
    fun getTodayWaterSum(startOfDay: Long): Flow<Int?>
}