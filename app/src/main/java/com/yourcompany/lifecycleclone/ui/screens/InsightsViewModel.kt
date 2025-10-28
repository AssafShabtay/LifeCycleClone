package com.yourcompany.lifecycleclone.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.core.repository.VisitRepository
import com.yourcompany.lifecycleclone.insights.CategoryBreakdown
import com.yourcompany.lifecycleclone.insights.InsightsRepository
import com.yourcompany.lifecycleclone.insights.SleepCorrelation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.DayOfWeek

/**
 * ViewModel backing the insights screen.
 *
 * Exposes:
 * - weeklyBreakdown: breakdown by category for the current ISO week (Mon–Sun)
 * - sleepCorrelations: correlation between last activity and sleep
 */
class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val insightsRepository: InsightsRepository

    val weeklyBreakdown: StateFlow<List<CategoryBreakdown>>

    // Correlations between sleep sessions and the last activity before sleep
    private val _sleepCorrelations =
        MutableStateFlow<List<SleepCorrelation>>(emptyList())
    val sleepCorrelations: StateFlow<List<SleepCorrelation>> get() = _sleepCorrelations

    init {
        // You only need to grab the DB once
        val db = AppDatabase.getInstance(application)
        val visitDao = db.visitDao()
        val sleepSessionDao = db.sleepSessionDao()

        val visitRepository = VisitRepository(visitDao)
        insightsRepository = InsightsRepository(visitDao)

        // Figure out the current ISO week range [startOfWeek, endOfWeek)
        val now: ZonedDateTime = ZonedDateTime.now()

        // Move "now" to Monday of this week, then snap to start of that day in the same zone
        val startOfWeekMillis = now
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toLocalDate()
            .atStartOfDay(now.zone)
            .toInstant()
            .toEpochMilli()

        val endOfWeekMillis =
            startOfWeekMillis + 7L * 24L * 60L * 60L * 1000L // +7 days in ms

        // Flow<List<CategoryBreakdown>> -> StateFlow<List<CategoryBreakdown>>
        weeklyBreakdown = visitRepository
            .observeVisitsInRange(startOfWeekMillis, endOfWeekMillis)
            .map {
                // Recompute the breakdown whenever visits change.
                runCatching {
                    insightsRepository.getBreakdown(startOfWeekMillis, endOfWeekMillis)
                }.getOrElse { emptyList() }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                // IMPORTANT: give the compiler the concrete type
                initialValue = emptyList<CategoryBreakdown>()
            )

        // One-off load of correlations for this week
        viewModelScope.launch {
            val correlations = insightsRepository.getSleepCorrelations(
                startOfWeekMillis,
                endOfWeekMillis,
                sleepSessionDao
            )
            _sleepCorrelations.value = correlations
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Use ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY,
                // not AndroidViewModel.APPLICATION_KEY
                val app = this[
                    ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                ] as Application

                InsightsViewModel(app)
            }
        }
    }
}
