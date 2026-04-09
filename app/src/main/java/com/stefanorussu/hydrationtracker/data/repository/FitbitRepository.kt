package com.stefanorussu.hydrationtracker.data.repository

import android.content.Context
import com.stefanorussu.hydrationtracker.data.local.WaterDao
import com.stefanorussu.hydrationtracker.data.network.FitbitApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FitbitRepository(
    val context: Context,
    private val waterDao: WaterDao
) {
    private val prefs = context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE)

    private val api: FitbitApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.fitbit.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FitbitApi::class.java)
    }

    private fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    // --- SALVATAGGIO ORARIO SYNC ---
    fun getLastSyncTime(): String? {
        return prefs.getString("last_sync_time", null)
    }

    fun saveLastSyncTime(time: String) {
        prefs.edit().putString("last_sync_time", time).apply()
    }
    // -------------------------------

    // --- ESTRAZIONE PESO ---
    suspend fun fetchFitbitWeight(): Float? {
        val token = getAccessToken() ?: return null
        return try {
            val response = api.getUserProfile("Bearer $token")
            response.user.weight
        } catch (e: Exception) {
            android.util.Log.e("FITBIT_SYNC", "Errore estrazione peso: ${e.message}")
            null
        }
    }
    // -----------------------

    suspend fun syncSingleRecordToFitbit(amountMl: Int, timestamp: Long): String? {
        val token = getAccessToken() ?: return null

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormatter.format(Date(timestamp))

        return try {
            val response = api.logWater("Bearer $token", dateString, amountMl)
            android.util.Log.d("FITBIT_SYNC", "SUCCESSO! Acqua inviata a Fitbit")
            response.waterLog.logId.toString()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                android.util.Log.w("FITBIT_SYNC", "Token scaduto. Tento il rinnovo automatico...")

                val authManager = com.stefanorussu.hydrationtracker.data.network.FitbitAuthManager(context)
                val refreshSuccess = authManager.refreshAccessToken()

                if (refreshSuccess) {
                    val newToken = getAccessToken()
                    try {
                        val responseRetry = api.logWater("Bearer $newToken", dateString, amountMl)
                        android.util.Log.d("FITBIT_SYNC", "SUCCESSO DOPO RINNOVO! Acqua inviata a Fitbit")
                        return responseRetry.waterLog.logId.toString()
                    } catch (eRetry: Exception) {
                        android.util.Log.e("FITBIT_SYNC", "Fallito anche il secondo tentativo.")
                        null
                    }
                } else {
                    android.util.Log.e("FITBIT_SYNC", "Rinnovo del token fallito in modo critico.")
                    null
                }
            } else {
                android.util.Log.e("FITBIT_SYNC", "Errore HTTP non 401: ${e.code()}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FITBIT_SYNC", "Errore Generico: ${e.message}")
            null
        }
    }

    suspend fun deleteRecordFromFitbit(fitbitLogId: String): Boolean {
        val token = getAccessToken() ?: return false

        return try {
            val response = api.deleteWaterLog("Bearer $token", fitbitLogId)

            if (response.isSuccessful) {
                android.util.Log.d("FITBIT_SYNC", "SUCCESSO! Acqua eliminata da Fitbit.")
                true
            } else if (response.code() == 401) {
                android.util.Log.w("FITBIT_SYNC", "Token scaduto durante l'eliminazione. Rinnovo...")
                val authManager = com.stefanorussu.hydrationtracker.data.network.FitbitAuthManager(context)

                if (authManager.refreshAccessToken()) {
                    val newToken = getAccessToken()
                    val retryResponse = api.deleteWaterLog("Bearer $newToken", fitbitLogId)
                    retryResponse.isSuccessful
                } else {
                    false
                }
            } else {
                android.util.Log.e("FITBIT_SYNC", "Errore HTTP durante eliminazione: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("FITBIT_SYNC", "Errore Generico: ${e.message}")
            false
        }
    }

    suspend fun syncTodayWater(waterDao: com.stefanorussu.hydrationtracker.data.local.WaterDao): Boolean {
        val token = getAccessToken() ?: return false

        val todayString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

        return try {
            val response = api.getWaterLogs("Bearer $token", todayString)

            val fitbitLogs = response.water
            var addedCount = 0
            var deletedCount = 0

            fitbitLogs.forEach { fitbitEntry ->
                val count = waterDao.checkFitbitIdCount(fitbitEntry.logId.toString())

                if (count == 0) {
                    val newRecord = com.stefanorussu.hydrationtracker.data.local.WaterRecord(
                        amountMl = fitbitEntry.amount,
                        timestamp = System.currentTimeMillis(),
                        drinkName = "Acqua",
                        source = "FITBIT",
                        externalId = fitbitEntry.logId.toString()
                    )
                    waterDao.insert(newRecord)
                    addedCount++
                }
            }

            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0); calendar.set(java.util.Calendar.MINUTE, 0); calendar.set(java.util.Calendar.SECOND, 0); calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23); calendar.set(java.util.Calendar.MINUTE, 59); calendar.set(java.util.Calendar.SECOND, 59); calendar.set(java.util.Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            val localSyncedRecords = waterDao.getSyncedRecordsForToday(startOfDay, endOfDay)
            val fitbitIds = fitbitLogs.map { it.logId.toString() }

            localSyncedRecords.forEach { localRecord ->
                if (localRecord.externalId !in fitbitIds) {
                    waterDao.deleteWater(localRecord)
                    deletedCount++
                }
            }

            android.util.Log.d("FITBIT_SYNC", "Sincronizzazione completata! Aggiunti $addedCount, Eliminati $deletedCount record.")
            true

        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                android.util.Log.w("FITBIT_SYNC", "Token scaduto durante il download. Rinnovo in corso...")
                val authManager = com.stefanorussu.hydrationtracker.data.network.FitbitAuthManager(context)

                if (authManager.refreshAccessToken()) {
                    syncTodayWater(waterDao)
                } else {
                    false
                }
            } else {
                android.util.Log.e("FITBIT_SYNC", "Errore dal server: ${e.code()}")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("FITBIT_SYNC", "Nessuna connessione a internet: ${e.message}")
            false
        }
    }
}