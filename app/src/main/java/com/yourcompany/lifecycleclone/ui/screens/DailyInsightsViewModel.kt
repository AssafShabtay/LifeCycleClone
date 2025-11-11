package com.yourcompany.lifecycleclone.ui.screens

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.core.db.VisitWithPlace
import com.yourcompany.lifecycleclone.core.repository.VisitRepository
import com.yourcompany.lifecycleclone.ui.util.getColorForCategory
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Represents a slice of activity time used by the pie chart and day list.
 */
data class ActivityTimeSlot(
    val activityType: String,
    val startTime: Date,
    val endTime: Date,
    val durationMillis: Long,
    val color: Color,
    val isActive: Boolean = false,
    val dayStart: Date,
    val originalStartTime: Date = startTime,
    val originalEndTime: Date = endTime,
    val placeName: String? = null,
    val placeCategory: String? = null
)

/**
 * UI state exposed to the insights screen.
 */
data class DailyInsightsUiState(
    val selectedDate: LocalDate,
    val displayedMonth: YearMonth,
    val daySlots: List<ActivityTimeSlot>,
    val monthActivityMap: Map<Int, List<ActivityTimeSlot>>,
    val isDayLoading: Boolean,
    val isMonthLoading: Boolean
)

private data class CombinedState(
    val selectedDate: LocalDate,
    val displayedMonth: YearMonth,
    val daySlots: List<ActivityTimeSlot>,
    val monthActivityMap: Map<Int, List<ActivityTimeSlot>>,
    val isDayLoading: Boolean
)

class DailyInsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val visitRepository: VisitRepository
    private val zoneId: ZoneId = ZoneId.systemDefault()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    private val _daySlots = MutableStateFlow<List<ActivityTimeSlot>>(emptyList())
    private val _monthActivityMap = MutableStateFlow<Map<Int, List<ActivityTimeSlot>>>(emptyMap())
    private val _isDayLoading = MutableStateFlow(false)
    private val _isMonthLoading = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(
        DailyInsightsUiState(
            selectedDate = _selectedDate.value,
            displayedMonth = _displayedMonth.value,
            daySlots = emptyList(),
            monthActivityMap = emptyMap(),
            isDayLoading = true,
            isMonthLoading = true
        )
    )

    val uiState: StateFlow<DailyInsightsUiState> = _uiState.asStateFlow()

    init {
        val dao = AppDatabase.getInstance(application).visitDao()
        visitRepository = VisitRepository(dao)

        viewModelScope.launch { loadDay(_selectedDate.value) }
        viewModelScope.launch { loadMonth(_displayedMonth.value) }

        viewModelScope.launch {
            combine(
                combine(
                    _selectedDate,
                    _displayedMonth,
                    _daySlots,
                    _monthActivityMap,
                    _isDayLoading
                ) { selected, month, daySlots, monthMap, dayLoading ->
                    CombinedState(
                        selectedDate = selected,
                        displayedMonth = month,
                        daySlots = daySlots,
                        monthActivityMap = monthMap,
                        isDayLoading = dayLoading
                    )
                },
                _isMonthLoading
            ) { combined, monthLoading ->
                DailyInsightsUiState(
                    selectedDate = combined.selectedDate,
                    displayedMonth = combined.displayedMonth,
                    daySlots = combined.daySlots,
                    monthActivityMap = combined.monthActivityMap,
                    isDayLoading = combined.isDayLoading,
                    isMonthLoading = monthLoading
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onDateSelected(newDate: LocalDate) {
        if (_selectedDate.value == newDate) return
        _selectedDate.value = newDate
        val newMonth = YearMonth.from(newDate)
        if (_displayedMonth.value != newMonth) {
            _displayedMonth.value = newMonth
        viewModelScope.launch { loadMonth(newMonth) }
        }
        viewModelScope.launch { loadDay(newDate) }
    }

    fun onDisplayedMonthChanged(newMonth: YearMonth) {
        if (_displayedMonth.value == newMonth) return
        _displayedMonth.value = newMonth
        viewModelScope.launch { loadMonth(newMonth) }
        if (YearMonth.from(_selectedDate.value) != newMonth) {
            val adjustedDay = newMonth.atDay(_selectedDate.value.dayOfMonth.coerceIn(1, newMonth.lengthOfMonth()))
            _selectedDate.value = adjustedDay
        viewModelScope.launch { loadDay(adjustedDay) }
        }
    }

    private suspend fun loadDay(date: LocalDate) {
        _isDayLoading.value = true
        try {
            val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val visits = visitRepository.getVisitsInRange(start, end)
            _daySlots.value = visits.toSlotsWithin(start, end)
        } finally {
            _isDayLoading.value = false
        }
    }

    private suspend fun loadMonth(month: YearMonth) {
        _isMonthLoading.value = true
        try {
            val start = month.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val visits = visitRepository.getVisitsInRange(start, end)
            val slotsByDay = mutableMapOf<Int, MutableList<ActivityTimeSlot>>()
            visits.toSlotsWithin(start, end).forEach { slot ->
                val day = Instant.ofEpochMilli(slot.startTime.time).atZone(zoneId).dayOfMonth
                slotsByDay.getOrPut(day) { mutableListOf() }.add(slot)
            }
            _monthActivityMap.value = slotsByDay.mapValues { entry ->
                entry.value.sortedBy { slot -> slot.startTime }
            }
        } finally {
            _isMonthLoading.value = false
        }
    }

    private fun List<VisitWithPlace>.toSlotsWithin(rangeStart: Long, rangeEnd: Long): List<ActivityTimeSlot> {
        if (isEmpty()) return emptyList()
        val nowMillis = System.currentTimeMillis()
        val slots = mutableListOf<ActivityTimeSlot>()
        for (visit in this) {
            val visitEnd = visit.endTime ?: nowMillis
            val clampedEnd = minOf(visitEnd, rangeEnd)
            var currentStart = maxOf(visit.startTime, rangeStart)
            if (clampedEnd <= currentStart) continue
            while (currentStart < clampedEnd) {
                val currentDate = Instant.ofEpochMilli(currentStart).atZone(zoneId).toLocalDate()
                val dayStart = currentDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val dayEnd = currentDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val segmentEnd = minOf(clampedEnd, dayEnd)
                val duration = segmentEnd - currentStart
                if (duration <= 0L) {
                    currentStart = segmentEnd
                    continue
                }
                slots += ActivityTimeSlot(
                    activityType = visit.placeLabel,
                    startTime = Date(currentStart),
                    endTime = Date(segmentEnd),
                    durationMillis = duration,
                    color = getColorForCategory(visit.placeCategory),
                    isActive = visit.endTime == null,
                    dayStart = Date(dayStart),
                    originalStartTime = Date(visit.startTime),
                    originalEndTime = Date(visitEnd),
                    placeName = visit.placeLabel,
                    placeCategory = visit.placeCategory
                )
                currentStart = segmentEnd
            }
        }
        return slots.sortedBy { it.startTime }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                DailyInsightsViewModel(app)
            }
        }
    }
}








