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

class WaterViewModel(
    private val waterRepository: WaterRepository,
    private val fitbitRepository: FitbitRepository? = null
) : ViewModel() {

    private val _dayStart = MutableStateFlow(calculateStartOfDay())

    // --- VARIABILI PER LA UI DELLA HOME ---
    // Inizializzato a null. Il valore verrà caricato dalla UI chiamando loadLastSyncTime()
    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime

    private val _smartMessageOverride = MutableStateFlow<String?>(null)
    val smartMessageOverride: StateFlow<String?> = _smartMessageOverride
    // --------------------------------------

    // --- MOTORE METEO ---
    private val weatherRepository = com.stefanorussu.hydrationtracker.data.repository.WeatherRepository()

    private val _currentTemp = kotlinx.coroutines.flow.MutableStateFlow<Double?>(null)
    val currentTemp: kotlinx.coroutines.flow.StateFlow<Double?> = _currentTemp

    private val _weatherBonus = kotlinx.coroutines.flow.MutableStateFlow(0)
    val weatherBonus: kotlinx.coroutines.flow.StateFlow<Int> = _weatherBonus

    fun fetchWeather() {
        viewModelScope.launch(Dispatchers.IO) {
            val temp = weatherRepository.getSassariTemperature()
            _currentTemp.value = temp

            // Se fanno 28 gradi o più, aggiungiamo 400ml di obiettivo!
            if (temp != null && temp >= 28.0) {
                _weatherBonus.value = 400
            } else {
                _weatherBonus.value = 0
            }
        }
    }
    // --------------------

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
        val currentStart = _dayStart.value
        val newStart = calculateStartOfDay()

        if (currentStart != newStart) {
            _dayStart.value = newStart
            _smartMessageOverride.value = null
            android.util.Log.d("WATER_VIEWMODEL", "Mezzanotte passata: Grafici resettati.")
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
    fun syncWithFitbit(context: Context, profileViewModel: com.stefanorussu.hydrationtracker.ui.viewmodel.ProfileViewModel, isManual: Boolean = false) {
        if (_isRefreshing.value) return
        _isRefreshing.value = true

        viewModelScope.launch {
            try {
                // Creiamo il repository localmente qui usando il context passato
                val database = com.stefanorussu.hydrationtracker.data.local.AppDatabase.getDatabase(context)
                val fitbitRepo = com.stefanorussu.hydrationtracker.data.repository.FitbitRepository(context, database.waterDao())

                val success = fitbitRepo.syncTodayWater(database.waterDao())

                if (success) {
                    val timeString = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    _lastSyncTime.value = timeString

                    // Salviamo il tempo nelle SharedPreferences usando il context
                    val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("last_sync_time", timeString).apply()

                    if (isManual) {
                        _snackbarMessage.emit("Sincronizzazione completata")
                    }

                    // --- CONTROLLO SPORT ---
                    val sportData = fitbitRepo.calculateSportWaterBonus()
                    val activeMinutes = sportData.first
                    val extraWater = sportData.second

                    if (activeMinutes > 0 && extraWater > 0) {
                        _smartMessageOverride.value = "Ho rilevato $activeMinutes min di attività! 🔥 Ho calcolato +${extraWater}ml per il recupero."
                        _weatherBonus.value += extraWater // Usiamo la variabile bonus per aggiungere l'acqua al traguardo
                    }

                    // --- CONTROLLO PESO AUTOMATICO ---
                    val fitbitProfile = fitbitRepo.fetchFitbitProfile()
                    if (fitbitProfile != null) {
                        val newWeight = fitbitProfile.weight.toFloat()

                        // Recuperiamo il profilo attuale dal ViewModel
                        val currentProfile = profileViewModel.userProfile.value

                        if (currentProfile.weightKg != newWeight) {
                            val updatedProfile = currentProfile.copy(weightKg = newWeight)

                            // Usiamo la tua funzione updateProfile senza causare errori
                            profileViewModel.updateProfile(context, updatedProfile)
                        }
                    }

                } else if (isManual) {
                    _snackbarMessage.emit("Errore di sincronizzazione")
                }
            } catch (e: Exception) {
                android.util.Log.e("WaterViewModel", "Errore sync", e)
                if (isManual) _snackbarMessage.emit("Errore: ${e.localizedMessage}")
            } finally {
                _isRefreshing.value = false
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

    // Carica il tempo all'avvio dell'app
    fun loadLastSyncTime(context: Context) {
        val prefs = context.getSharedPreferences("hydration_prefs", Context.MODE_PRIVATE)
        val savedTime = prefs.getString("last_sync_time", null)
        if (savedTime != null) {
            _lastSyncTime.value = savedTime
        }
    }
}