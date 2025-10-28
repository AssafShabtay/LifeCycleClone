package com.yourcompany.lifecycleclone.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.core.repository.VisitRepository // (only needed if WeeklyJournalRepository uses it indirectly)
import com.yourcompany.lifecycleclone.core.repository.WeeklyJournalRepository
import com.yourcompany.lifecycleclone.core.model.WeeklyJournalEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

/**
 * ViewModel for the weekly journal screen.
 *
 * It retrieves the journal summary for the most recent ISO week (Mon–Sun).
 * If none exists, it generates one.
 */
class JournalViewModel(application: Application) : AndroidViewModel(application) {

    private val journalRepository: WeeklyJournalRepository

    private val _journal = MutableStateFlow<WeeklyJournalEntity?>(null)
    val journal: StateFlow<WeeklyJournalEntity?> get() = _journal

    init {
        val db = AppDatabase.getInstance(application)
        journalRepository = WeeklyJournalRepository(
            db.visitDao(),
            db.weeklyJournalDao()
        )
        loadLatestJournal()
    }

    private fun loadLatestJournal() {
        viewModelScope.launch {
            // Start of the CURRENT ISO week (Monday), expressed as epochDay (Long)
            val now: ZonedDateTime = ZonedDateTime.now()
            val startOfWeekDate = now
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()

            val weekStartEpochDay: Long = startOfWeekDate.toEpochDay()

            // Try to load an existing summary for this week
            var entity = journalRepository.getWeeklySummary(weekStartEpochDay)

            if (entity == null) {
                // Generate one if it's missing
                entity = journalRepository.generateWeeklySummary(weekStartEpochDay)
            }

            _journal.value = entity
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                // IMPORTANT: use ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                val app = this[
                    ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                ] as Application
                JournalViewModel(app)
            }
        }
    }
}
