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

    // Preleva il Token salvato
    private fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    /**
     * Invia un record d'acqua a Fitbit e salva l'ID esterno restituito.
     */
    suspend fun syncSingleRecordToFitbit(amountMl: Int, timestamp: Long): String? {
        val token = getAccessToken() ?: return null

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormatter.format(Date(timestamp))

        return try {
            // Tentativo 1: Invio normale
            val response = api.logWater("Bearer $token", dateString, amountMl)
            android.util.Log.d("FITBIT_SYNC", "SUCCESSO! Acqua inviata a Fitbit")
            response.waterLog.logId.toString()
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                android.util.Log.w("FITBIT_SYNC", "Token scaduto. Tento il rinnovo automatico...")

                // Il token è scaduto! Proviamo a rinnovarlo
                val authManager = com.stefanorussu.hydrationtracker.data.network.FitbitAuthManager(context)
                val refreshSuccess = authManager.refreshAccessToken()

                if (refreshSuccess) {
                    // Tentativo 2: Se il rinnovo è andato bene, riproviamo l'invio con il NUOVO token
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
            // Tentativo 1: Eliminazione normale
            val response = api.deleteWaterLog("Bearer $token", fitbitLogId)

            if (response.isSuccessful) {
                android.util.Log.d("FITBIT_SYNC", "SUCCESSO! Acqua eliminata da Fitbit.")
                true
            } else if (response.code() == 401) {
                // Il token è scaduto! Proviamo a rinnovarlo
                android.util.Log.w("FITBIT_SYNC", "Token scaduto durante l'eliminazione. Rinnovo...")
                val authManager = com.stefanorussu.hydrationtracker.data.network.FitbitAuthManager(context)

                if (authManager.refreshAccessToken()) {
                    // Tentativo 2 con il nuovo token
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

    // Sincronizza i dati di Fitbit con il telefono (Aggiunte ed Eliminazioni)
    suspend fun syncTodayWater(waterDao: com.stefanorussu.hydrationtracker.data.local.WaterDao): Boolean {
        val token = getAccessToken() ?: return false

        // Formattiamo la data di oggi come vuole Fitbit: "yyyy-MM-dd"
        val todayString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

        return try {
            val response = api.getWaterLogs("Bearer $token", todayString)

            val fitbitLogs = response.water
            var addedCount = 0
            var deletedCount = 0

            // 1. FASE DI AGGIUNTA: Analizziamo ogni bevanda che Fitbit ci ha inviato
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

            // 2. FASE DI ELIMINAZIONE: Controlliamo cosa non c'è più
            // Calcoliamo inizio e fine della giornata odierna
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0); calendar.set(java.util.Calendar.MINUTE, 0); calendar.set(java.util.Calendar.SECOND, 0); calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 23); calendar.set(java.util.Calendar.MINUTE, 59); calendar.set(java.util.Calendar.SECOND, 59); calendar.set(java.util.Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            // Prendiamo tutte le bevande del telefono di oggi che hanno un ID Fitbit
            val localSyncedRecords = waterDao.getSyncedRecordsForToday(startOfDay, endOfDay)

            // Creiamo una lista veloce con solo gli ID ricevuti da Fitbit per confrontarli
            val fitbitIds = fitbitLogs.map { it.logId.toString() }

            // Se una bevanda sul telefono non è più nella lista di Fitbit, la eliminiamo
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
                    syncTodayWater(waterDao) // Riprova con il nuovo token
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