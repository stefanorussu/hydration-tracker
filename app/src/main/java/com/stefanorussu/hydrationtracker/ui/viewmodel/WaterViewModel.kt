package com.stefanorussu.hydrationtracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stefanorussu.hydrationtracker.data.local.DrinkFrequency
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import com.stefanorussu.hydrationtracker.data.repository.WaterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar
import com.stefanorussu.hydrationtracker.data.repository.FitbitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaterViewModel(
    private val waterRepository: WaterRepository,
    private val fitbitRepository: FitbitRepository? = null
) : ViewModel() {

    private val _dayStart = MutableStateFlow(calculateStartOfDay())

    // --- VARIABILI PER LA UI DELLA HOME ---
    private val _lastSyncTime = MutableStateFlow<String?>(fitbitRepository?.getLastSyncTime())
    val lastSyncTime: StateFlow<String?> = _lastSyncTime

    private val _smartMessageOverride = MutableStateFlow<String?>(null)
    val smartMessageOverride: StateFlow<String?> = _smartMessageOverride
    // --------------------------------------

    private fun calculateStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun calculateEndOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun checkMidnight() {
        val newStart = calculateStartOfDay()
        if (_dayStart.value != newStart) {
            _dayStart.value = newStart
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayTotal: Flow<Int?> = _dayStart.flatMapLatest { start ->
        waterRepository.getTodayTotal(start, calculateEndOfDay())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyRecords: Flow<List<WaterRecord>> = _dayStart.flatMapLatest { start ->
        waterRepository.getRecordsBetweenDates(start, calculateEndOfDay())
    }

    val drinkFrequencies: Flow<List<DrinkFrequency>> = waterRepository.getDrinkFrequencies()

    fun addWater(amount: Int, originalInput: Int, drinkName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newRecord = WaterRecord(
                amountMl = amount,
                inputAmountMl = originalInput,
                timestamp = System.currentTimeMillis(),
                drinkName = drinkName
            )

            val generatedId = waterRepository.insert(newRecord)

            fitbitRepository?.let { fitbit ->
                val fitbitLogId = fitbit.syncSingleRecordToFitbit(amount, newRecord.timestamp)

                if (fitbitLogId != null) {
                    val recordWithFitbitId = newRecord.copy(
                        id = generatedId.toInt(),
                        externalId = fitbitLogId
                    )
                    waterRepository.updateWater(recordWithFitbitId)
                }
            }
        }
    }

    fun updateRecord(record: WaterRecord, newAmount: Int, newTimestamp: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            waterRepository.updateWater(record.copy(amountMl = newAmount, timestamp = newTimestamp))
        }
    }

    fun deleteWater(record: WaterRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            waterRepository.deleteWater(record)

            if (record.externalId != null) {
                fitbitRepository?.let { fitbit ->
                    fitbit.deleteRecordFromFitbit(record.externalId)
                }
            }
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _snackbarMessage = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val snackbarMessage: kotlinx.coroutines.flow.SharedFlow<String> = _snackbarMessage

    // --- NUOVA SINCRONIZZAZIONE INTELLIGENTE ---
    fun syncWithFitbit(context: Context, profileViewModel: ProfileViewModel, isManual: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            fitbitRepository?.let { fitbit ->
                if (isManual) _isRefreshing.value = true

                // 1. Sincronizza l'acqua
                val db = com.stefanorussu.hydrationtracker.data.local.AppDatabase.getDatabase(fitbit.context)
                val success = fitbit.syncTodayWater(db.waterDao())

                // 2. Sincronizza il Peso
                val fitbitWeight = fitbit.fetchFitbitWeight()
                if (fitbitWeight != null) {
                    val didWeightChange = profileViewModel.updateWeightFromFitbit(context, fitbitWeight)
                    if (didWeightChange) {
                        // Accende il messaggio speciale sul Pannello Smart per 6 secondi
                        _smartMessageOverride.value = "Peso aggiornato da Fitbit. Obiettivo ricalcolato! ✨"
                        launch(Dispatchers.Main) {
                            delay(6000)
                            _smartMessageOverride.value = null
                        }
                    }
                }

                // 3. Aggiorna orario TopBar
                if (success) {
                    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    fitbit.saveLastSyncTime(timeString)
                    _lastSyncTime.value = timeString
                }

                if (isManual) {
                    _isRefreshing.value = false
                    if (success) {
                        _snackbarMessage.emit("Sincronizzato con Fitbit ⌚")
                    } else {
                        _snackbarMessage.emit("Controlla la connessione internet ⚠️")
                    }
                }
            }
        }
    }
    // -------------------------------------------

    fun getSuggestedAmount(drinkName: String, onResult: (Int?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val suggested = waterRepository.getMostFrequentAmount(drinkName)
            withContext(Dispatchers.Main) {
                onResult(suggested)
            }
        }
    }
}