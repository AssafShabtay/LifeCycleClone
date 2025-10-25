package com.yourcompany.lifecycleclone.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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

/**
 * ViewModel backing the insights screen. It exposes a [StateFlow] of category breakdowns
 * for the current ISO week (Monday–Sunday).
 */
class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val insightsRepository: InsightsRepository
    val weeklyBreakdown: StateFlow<List<CategoryBreakdown>>

    // Correlations between sleep sessions and last activity before sleep
    private val _sleepCorrelations = MutableStateFlow<List<SleepCorrelation>>(emptyList())
    val sleepCorrelations: StateFlow<List<SleepCorrelation>> get() = _sleepCorrelations

    init {
        val db = AppDatabase.getInstance(application)
        val visitDao = db.visitDao()
        val sleepSessionDao = db.sleepSessionDao()

        val visitRepository = VisitRepository(visitDao)
        insightsRepository = InsightsRepository(visitDao)

        // Determine current ISO week range [startOfWeekMillis, endOfWeekMillis)
        val now = java.time.ZonedDateTime.now()
        val startOfWeek = now.toLocalDate()
            .with(java.time.DayOfWeek.MONDAY)
            .atStartOfDay(now.zone)
            .toInstant()
            .toEpochMilli()
        val endOfWeek =
            startOfWeek + 7L * 24L * 60L * 60L * 1000L // start + 7 days in ms

        // Stream the weekly breakdown
        weeklyBreakdown = visitRepository.observeVisitsInRange(startOfWeek, endOfWeek)
            .map {
                // Recompute breakdown for that range
                runCatching {
                    insightsRepository.getBreakdown(startOfWeek, endOfWeek)
                }.getOrElse { emptyList() }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

        // One-shot correlation calculation
        viewModelScope.launch {
            val correlations = insightsRepository.getSleepCorrelations(
                startOfWeek,
                endOfWeek,
                sleepSessionDao
            )
            _sleepCorrelations.value = correlations
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // Pull the Application out of CreationExtras using APPLICATION_KEY
                val app = this[APPLICATION_KEY] as Application
                InsightsViewModel(app)
            }
        }
    }
}
