package com.stefanorussu.hydrationtracker.ui.viewmodel

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

class WaterViewModel(
    private val waterRepository: WaterRepository,
    private val fitbitRepository: FitbitRepository? = null
) : ViewModel() {

    private val startOfDay: Long
    private val endOfDay: Long

    init {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startOfDay = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        endOfDay = calendar.timeInMillis
    }

    val todayTotal: Flow<Int?> = waterRepository.getTodayTotal(startOfDay, endOfDay)
    val dailyRecords: Flow<List<WaterRecord>> = waterRepository.getRecordsBetweenDates(startOfDay, endOfDay)
    val drinkFrequencies: Flow<List<DrinkFrequency>> = waterRepository.getDrinkFrequencies()

    fun addWater(amount: Int, originalInput: Int, drinkName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Creiamo il nuovo record con la distinzione tra acqua (amount) e liquido reale (originalInput)
            val newRecord = WaterRecord(
                amountMl = amount,
                inputAmountMl = originalInput,
                timestamp = System.currentTimeMillis(),
                drinkName = drinkName
            )

            // Salviamo il nuovo record e otteniamo l'ID dal database
            val generatedId = waterRepository.insert(newRecord)

            // Proviamo a inviare a Fitbit
            fitbitRepository?.let { fitbit ->
                // NOTA: Inviamo ad Fitbit l'acqua pura calcolata (amount), non il liquido lordo!
                val fitbitLogId = fitbit.syncSingleRecordToFitbit(amount, newRecord.timestamp)

                if (fitbitLogId != null) {
                    android.util.Log.d("FITBIT_SYNC", "Fitbit ha risposto con ID: $fitbitLogId. Lo salvo nel DB!")

                    // Aggiorniamo la riga locale con l'ID di Fitbit
                    val recordWithFitbitId = newRecord.copy(
                        id = generatedId.toInt(),
                        externalId = fitbitLogId
                    )
                    waterRepository.updateWater(recordWithFitbitId)
                } else {
                    android.util.Log.e("FITBIT_SYNC", "Fitbit non ha restituito un ID valido.")
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
        android.util.Log.d("FITBIT_SYNC", "👉 TASTO ELIMINA (HOME) PREMUTO! Cerco di eliminare: ${record.drinkName}")

        viewModelScope.launch(Dispatchers.IO) {
            waterRepository.deleteWater(record)

            if (record.externalId != null) {
                android.util.Log.d("FITBIT_SYNC", "Record con ID Fitbit ${record.externalId} trovato. Chiamo i server...")
                fitbitRepository?.let { fitbit ->
                    val success = fitbit.deleteRecordFromFitbit(record.externalId)
                    if (success) {
                        android.util.Log.d("FITBIT_SYNC", "SUCCESSO! Acqua eliminata da Fitbit dalla Home.")
                    } else {
                        android.util.Log.e("FITBIT_SYNC", "Eliminazione su Fitbit fallita.")
                    }
                }
            } else {
                android.util.Log.w("FITBIT_SYNC", "ATTENZIONE: Questo record ha l'ID vuoto! Non chiamo Fitbit.")
            }
        }
    }

    private val _isRefreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRefreshing: kotlinx.coroutines.flow.StateFlow<Boolean> = _isRefreshing

    private val _snackbarMessage = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val snackbarMessage: kotlinx.coroutines.flow.SharedFlow<String> = _snackbarMessage

    fun syncWithFitbit(isManual: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            fitbitRepository?.let { fitbit ->
                if (isManual) _isRefreshing.value = true

                android.util.Log.d("FITBIT_SYNC", "Inizio sincronizzazione (Manuale: $isManual)...")

                val db = com.stefanorussu.hydrationtracker.data.local.AppDatabase.getDatabase(fitbit.context)
                val success = fitbit.syncTodayWater(db.waterDao())

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

    fun getSuggestedAmount(drinkName: String, onResult: (Int?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val suggested = waterRepository.getMostFrequentAmount(drinkName)
            withContext(Dispatchers.Main) {
                onResult(suggested)
            }
        }
    }
}