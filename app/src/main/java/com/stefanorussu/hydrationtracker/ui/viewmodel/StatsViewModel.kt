package com.stefanorussu.hydrationtracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stefanorussu.hydrationtracker.data.local.DailyWaterStats
import com.stefanorussu.hydrationtracker.data.local.WaterRecord
import com.stefanorussu.hydrationtracker.data.repository.WaterRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class TimeTab {
    DAY, WEEK, MONTH, YEAR
}

data class DrillDownItem(
    val timestamp: Long,
    val title: String,
    val subtitle: String,
    val value: Int,
    val isAverage: Boolean,
    val targetTab: TimeTab
)

data class ChartBarItem(
    val label: String,
    val value: Int,
    val showLabel: Boolean = true
)

class StatsViewModel(private val repository: WaterRepository) : ViewModel() {

    private val _currentTab = MutableStateFlow(TimeTab.DAY)
    val currentTab: StateFlow<TimeTab> = _currentTab.asStateFlow()

    private val _currentDate = MutableStateFlow(Calendar.getInstance())

    private val _dateRangeText = MutableStateFlow("")
    val dateRangeText: StateFlow<String> = _dateRangeText.asStateFlow()

    private val _currentRecords = MutableStateFlow<List<WaterRecord>>(emptyList())
    val currentRecords: StateFlow<List<WaterRecord>> = _currentRecords.asStateFlow()

    private val _listItems = MutableStateFlow<List<DrillDownItem>>(emptyList())
    val listItems: StateFlow<List<DrillDownItem>> = _listItems.asStateFlow()

    private val _chartItems = MutableStateFlow<List<ChartBarItem>>(emptyList())
    val chartItems: StateFlow<List<ChartBarItem>> = _chartItems.asStateFlow()

    private val _summaryValue = MutableStateFlow(0)
    val summaryValue: StateFlow<Int> = _summaryValue.asStateFlow()

    private var dataJob: Job? = null
    private var recordsJob: Job? = null

    init {
        loadDataForCurrentTab()
    }

    fun setTab(tab: TimeTab) {
        _currentTab.value = tab
        _currentDate.value = Calendar.getInstance()
        loadDataForCurrentTab()
    }

    fun shiftTime(forward: Boolean) {
        val amount = if (forward) 1 else -1
        val newDate = _currentDate.value.clone() as Calendar

        when (_currentTab.value) {
            TimeTab.DAY -> newDate.add(Calendar.DAY_OF_YEAR, amount)
            TimeTab.WEEK -> newDate.add(Calendar.WEEK_OF_YEAR, amount)
            TimeTab.MONTH -> newDate.add(Calendar.MONTH, amount)
            TimeTab.YEAR -> newDate.add(Calendar.YEAR, amount)
        }

        _currentDate.value = newDate
        loadDataForCurrentTab()
    }

    fun drillDownTo(timeInMillis: Long, targetTab: TimeTab) {
        val newDate = Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
        _currentDate.value = newDate
        _currentTab.value = targetTab
        loadDataForCurrentTab()
    }

    // AGGIORNATO: Modifica quantità e timestamp
    fun updateRecord(record: WaterRecord, newAmount: Int, newTimestamp: Long) {
        viewModelScope.launch {
            repository.updateWater(record.copy(amountMl = newAmount, timestamp = newTimestamp))
            loadDataForCurrentTab()
        }
    }

    fun deleteRecord(record: WaterRecord) {
        viewModelScope.launch {
            repository.deleteWater(record)
            loadDataForCurrentTab()
        }
    }

    private fun loadDataForCurrentTab() {
        dataJob?.cancel()
        recordsJob?.cancel()

        val calendar = _currentDate.value.clone() as Calendar
        var startTimestamp = 0L
        var endTimestamp = 0L

        val dayFormatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val shortDayFormatter = SimpleDateFormat("d MMM", Locale.getDefault())
        val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val yearFormatter = SimpleDateFormat("yyyy", Locale.getDefault())

        when (_currentTab.value) {
            TimeTab.DAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
                startTimestamp = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
                endTimestamp = calendar.timeInMillis
                _dateRangeText.value = dayFormatter.format(calendar.time)
            }
            TimeTab.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
                startTimestamp = calendar.timeInMillis
                val startText = shortDayFormatter.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
                endTimestamp = calendar.timeInMillis
                val endText = shortDayFormatter.format(calendar.time)
                _dateRangeText.value = "$startText - $endText"
            }
            TimeTab.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
                startTimestamp = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
                endTimestamp = calendar.timeInMillis
                _dateRangeText.value = monthYearFormatter.format(calendar.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            TimeTab.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
                startTimestamp = calendar.timeInMillis
                calendar.set(Calendar.MONTH, 11); calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
                endTimestamp = calendar.timeInMillis
                _dateRangeText.value = yearFormatter.format(calendar.time)
            }
        }

        recordsJob = viewModelScope.launch {
            repository.getRecordsBetweenDates(startTimestamp, endTimestamp).collect { records ->
                if (_currentTab.value == TimeTab.DAY) {
                    _currentRecords.value = records
                    _summaryValue.value = records.sumOf { it.amountMl }
                }
            }
        }

        val tzOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()).toLong()

        dataJob = viewModelScope.launch {
            repository.getStatsBetweenDates(startTimestamp, endTimestamp, tzOffset).collect { stats ->
                if (_currentTab.value != TimeTab.DAY) {
                    processAggregatedData(stats, _currentTab.value, startTimestamp)
                }
            }
        }
    }

    private fun processAggregatedData(rawStats: List<DailyWaterStats>, tab: TimeTab, startTimestamp: Long) {
        val list = mutableListOf<DrillDownItem>()
        val chart = mutableListOf<ChartBarItem>()
        var totalForSummary = 0
        var itemsCount = 0

        val cal = Calendar.getInstance()
        val now = Calendar.getInstance()

        when (tab) {
            TimeTab.WEEK -> {
                cal.timeInMillis = startTimestamp
                val dayFmt = SimpleDateFormat("EE", Locale.getDefault())
                val fullFmt = SimpleDateFormat("dd MMM", Locale.getDefault())

                val statsMap = rawStats.associateBy {
                    val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                    c.get(Calendar.DAY_OF_YEAR)
                }

                for (i in 0..6) {
                    val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                    val stat = statsMap[currentDayOfYear]
                    val isToday = currentDayOfYear == now.get(Calendar.DAY_OF_YEAR) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)

                    val titleForList = if (isToday) "Oggi" else dayFmt.format(cal.time).replaceFirstChar { it.uppercase() }
                    val titleForChart = dayFmt.format(cal.time).take(1).uppercase(Locale.getDefault())

                    val volume = stat?.totalMl ?: 0

                    if (stat != null) {
                        list.add(DrillDownItem(stat.dateMillis, titleForList, fullFmt.format(cal.time), volume, false, TimeTab.DAY))
                        totalForSummary += volume
                        itemsCount++
                    }
                    chart.add(ChartBarItem(titleForChart, volume, true))
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            TimeTab.MONTH -> {
                cal.timeInMillis = startTimestamp
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                val statsMap = rawStats.associateBy {
                    val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                    c.get(Calendar.DAY_OF_MONTH)
                }

                for (day in 1..maxDays) {
                    val volume = statsMap[day]?.totalMl ?: 0
                    val showLabel = day == 1 || day % 5 == 0 || day == maxDays
                    chart.add(ChartBarItem(day.toString(), volume, showLabel))
                }

                val groupedByWeek = rawStats.groupBy { stat ->
                    val c = Calendar.getInstance().apply { timeInMillis = stat.dateMillis }
                    c.set(Calendar.DAY_OF_WEEK, c.firstDayOfWeek); c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                    c.timeInMillis
                }
                val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
                groupedByWeek.forEach { (weekStartMillis, daysInWeek) ->
                    val weekEndMillis = Calendar.getInstance().apply { timeInMillis = weekStartMillis; add(Calendar.DAY_OF_YEAR, 6) }.timeInMillis
                    val title = "${fmt.format(Date(weekStartMillis))} - ${fmt.format(Date(weekEndMillis))}"
                    val weekTotal = daysInWeek.sumOf { it.totalMl }
                    val weekAvg = weekTotal / daysInWeek.size
                    list.add(DrillDownItem(weekStartMillis, title, "", weekAvg, true, TimeTab.WEEK))
                    totalForSummary += weekAvg
                    itemsCount++
                }
            }
            TimeTab.YEAR -> {
                val monthInitials = listOf("G", "F", "M", "A", "M", "G", "L", "A", "S", "O", "N", "D")
                val statsMap = rawStats.groupBy {
                    val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                    c.get(Calendar.MONTH)
                }

                val monthFmt = SimpleDateFormat("MMMM", Locale.getDefault())

                for (month in 0..11) {
                    val daysInMonth = statsMap[month] ?: emptyList()
                    val volumeAvg = if (daysInMonth.isNotEmpty()) daysInMonth.sumOf { it.totalMl } / daysInMonth.size else 0

                    chart.add(ChartBarItem(monthInitials[month], volumeAvg, true))

                    if (daysInMonth.isNotEmpty()) {
                        val c = Calendar.getInstance().apply { timeInMillis = startTimestamp; set(Calendar.MONTH, month) }
                        val title = monthFmt.format(c.time).replaceFirstChar { it.uppercase() }
                        list.add(DrillDownItem(c.timeInMillis, title, "", volumeAvg, true, TimeTab.MONTH))
                        totalForSummary += volumeAvg
                        itemsCount++
                    }
                }
            }
            TimeTab.DAY -> { }
        }

        _listItems.value = list.sortedByDescending { it.timestamp }
        _chartItems.value = chart
        _summaryValue.value = if (itemsCount > 0) totalForSummary / itemsCount else 0
    }
}

class StatsViewModelFactory(private val repository: WaterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository) as T
        }
        throw IllegalArgumentException("Classe ViewModel sconosciuta")
    }
}