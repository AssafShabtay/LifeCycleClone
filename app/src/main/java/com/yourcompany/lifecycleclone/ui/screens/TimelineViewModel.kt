package com.yourcompany.lifecycleclone.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.core.db.VisitWithPlace
import com.yourcompany.lifecycleclone.core.repository.VisitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel backing the timeline screen.
 *
 * Exposes today's visits (with associated place info) as a StateFlow.
 */
class TimelineViewModel(application: Application) : AndroidViewModel(application) {

    private val visitRepository: VisitRepository

    val todayVisits: StateFlow<List<VisitWithPlace>>

    init {
        val dao = AppDatabase.getInstance(application).visitDao()
        visitRepository = VisitRepository(dao)

        // observeTodayVisits() is assumed to be Flow<List<VisitWithPlace>>
        todayVisits = visitRepository.observeTodayVisits()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList<VisitWithPlace>()
            )
    }

    companion object {
        /**
         * Factory that allows this ViewModel to be created with the application context.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[
                    ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                ] as Application
                TimelineViewModel(app)
            }
        }
    }
}
