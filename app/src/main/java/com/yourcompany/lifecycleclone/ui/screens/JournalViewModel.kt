package com.yourcompany.lifecycleclone.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.core.repository.VisitRepository
import com.yourcompany.lifecycleclone.core.repository.WeeklyJournalRepository
import com.yourcompany.lifecycleclone.core.model.WeeklyJournalEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * ViewModel for the weekly journal screen.  It retrieves the journal summary for the most
 * recent week.  If none exists, it generates a new summary on demand.  The summary JSON
 * fields are decoded into strings for display.
 */
class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val journalRepository: WeeklyJournalRepository
    private val _journal = MutableStateFlow<WeeklyJournalEntity?>(null)
    val journal: StateFlow<WeeklyJournalEntity?> get() = _journal

    init {
        val db = AppDatabase.getInstance(application)
        journalRepository = WeeklyJournalRepository(db.visitDao(), db.weeklyJournalDao())
        loadLatestJournal()
    }

    private fun loadLatestJournal() {
        viewModelScope.launch {
            // Determine start of the current ISO week
            val now = java.time.ZonedDateTime.now()
            val weekStart = now.toLocalDate().with(java.time.DayOfWeek.MONDAY).toEpochDay()
            var entity = journalRepository.getWeeklySummary(weekStart)
            if (entity == null) {
                // Generate a new summary if none is available yet
                entity = journalRepository.generateWeeklySummary(weekStart)
            }
            _journal.value = entity
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

                JournalViewModel(app)
            }
        }
    }
}