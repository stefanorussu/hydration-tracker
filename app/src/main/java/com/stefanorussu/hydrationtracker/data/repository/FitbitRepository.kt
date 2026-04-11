package com.stefanorussu.hydrationtracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.stefanorussu.hydrationtracker.data.network.FitbitApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FitbitRepository(private val context: Context, private val waterDao: com.stefanorussu.hydrationtracker.data.local.WaterDao) {

    private val prefs: SharedPreferences = context.getSharedPreferences("fitbit_prefs", Context.MODE_PRIVATE)

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.fitbit.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(FitbitApi::class.java)

    private fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    // --- TRUCCO ANTI-FANTASMA (Cestino Offline) ---
    private fun addPendingDelete(logId: String) {
        val set = prefs.getStringSet("pending_deletes", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.add(logId)
        prefs.edit().putStringSet("pending_deletes", set).apply()
    }

    private fun removePendingDelete(logId: String) {
        val set = prefs.getStringSet("pending_deletes", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.remove(logId)
        prefs.edit().putStringSet("pending_deletes", set).apply()
    }

    private fun isPendingDelete(logId: String): Boolean {
        return prefs.getStringSet("pending_deletes", emptySet())?.contains(logId) == true
    }
    // ----------------------------------------------

    // --- ESTRAZIONE PESO ---
    suspend fun fetchFitbitProfile(): com.stefanorussu.hydrationtracker.data.network.FitbitUser? {
        val token = getAccessToken() ?: return null
        return try {
            val response = api.getUserProfile("Bearer $token")
            response.user
        } catch (e: Exception) {
            android.util.Log.e("FITBIT_SYNC", "Errore estrazione profilo: ${e.message}")
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
        addPendingDelete(fitbitLogId)

        val token = getAccessToken() ?: return false

        return try {
            val response = api.deleteWaterLog("Bearer $token", fitbitLogId)

            if (response.isSuccessful) {
                android.util.Log.d("FITBIT_SYNC", "SUCCESSO! Acqua eliminata da Fitbit.")
                removePendingDelete(fitbitLogId)
                true
            } else if (response.code() == 401) {
                android.util.Log.w("FITBIT_SYNC", "Token scaduto durante l'eliminazione. Rinnovo...")
                val authManager = com.stefanorussu.hydrationtracker.data.network.FitbitAuthManager(context)

                if (authManager.refreshAccessToken()) {
                    val newToken = getAccessToken()
                    val retryResponse = api.deleteWaterLog("Bearer $newToken", fitbitLogId)
                    if (retryResponse.isSuccessful) {
                        removePendingDelete(fitbitLogId)
                        true
                    } else false
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
                val logIdStr = fitbitEntry.logId.toString()

                if (isPendingDelete(logIdStr)) {
                    android.util.Log.d("FITBIT_SYNC", "FANTASMA INTERCETTATO ($logIdStr)! Forzo l'eliminazione su Fitbit.")
                    deleteRecordFromFitbit(logIdStr)
                } else {
                    val count = waterDao.checkFitbitIdCount(logIdStr)

                    if (count == 0) {
                        val newRecord = com.stefanorussu.hydrationtracker.data.local.WaterRecord(
                            amountMl = fitbitEntry.amount,
                            timestamp = System.currentTimeMillis(),
                            drinkName = "Acqua",
                            source = "FITBIT",
                            externalId = logIdStr
                        )
                        waterDao.insert(newRecord)
                        addedCount++
                    }
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
                if (localRecord.externalId !in fitbitIds && !isPendingDelete(localRecord.externalId ?: "")) {
                    waterDao.deleteWater(localRecord)
                    deletedCount++
                }
            }

            val unsyncedRecords = waterDao.getUnsyncedRecordsForToday(startOfDay, endOfDay)
            var uploadedCount = 0

            unsyncedRecords.forEach { localRecord ->
                val newFitbitId = syncSingleRecordToFitbit(localRecord.amountMl, localRecord.timestamp)
                if (newFitbitId != null) {
                    waterDao.updateWater(localRecord.copy(externalId = newFitbitId))
                    uploadedCount++
                }
            }

            android.util.Log.d("FITBIT_SYNC", "Sincronizzazione completata! Aggiunti +$addedCount da Fitbit, -$deletedCount eliminati, +$uploadedCount orfani caricati.")
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

    suspend fun syncUserProfile(profileViewModel: com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel) {
        val token = getAccessToken() ?: return
        try {
            val response = api.getUserProfile("Bearer $token")
            val user = response.user

            val newProfile = com.stefanorussu.hydrationtracker.data.local.UserProfile(
                weightKg = user.weight.toFloat(),
                birthDate = user.dateOfBirth,
                isMale = user.gender.lowercase() == "male",
                activityLevel = com.stefanorussu.hydrationtracker.data.local.ActivityLevel.MODERATE
            )

            profileViewModel.updateProfile(context, newProfile)
        } catch (e: Exception) {
            android.util.Log.e("FITBIT_SYNC", "Errore sync profilo: ${e.message}")
        }
    }

    // --- ALGORITMO SPORT INTELLIGENTE ---
    // Ritorna una Pair(minuti_totali, ml_da_aggiungere)
    suspend fun calculateSportWaterBonus(): Pair<Int, Int> {
        val token = getAccessToken() ?: return Pair(0, 0)
        return try {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val response = api.getActivities("Bearer $token", today)

            // Se per qualche motivo questi dati mancano, usiamo 0
            val intenseMinutes = response.summary.veryActiveMinutes ?: 0
            val moderateMinutes = response.summary.fairlyActiveMinutes ?: 0

            val totalActiveMinutes = intenseMinutes + moderateMinutes

            // Se hai fatto meno di 15 minuti di attività totale, ignoriamo (es. corsetta per prendere il bus)
            if (totalActiveMinutes < 15) return Pair(0, 0)

            // 15ml per minuto intenso, 5ml per minuto moderato
            val extraWaterMl = (intenseMinutes * 15) + (moderateMinutes * 5)

            Pair(totalActiveMinutes, extraWaterMl)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }
}