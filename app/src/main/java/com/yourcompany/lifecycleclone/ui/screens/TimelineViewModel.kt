package com.yourcompany.lifecycleclone.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.core.repository.VisitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel backing the timeline screen.  It exposes a [StateFlow] of visits for the current
 * day.  Dependencies are obtained from the [Application] context via [AppDatabase].
 */
class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val visitRepository: VisitRepository
    val todayVisits: StateFlow<List<com.yourcompany.lifecycleclone.core.db.VisitWithPlace>>

    init {
        val dao = AppDatabase.getInstance(application).visitDao()
        visitRepository = VisitRepository(dao)
        todayVisits = visitRepository.observeTodayVisits()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    companion object {
        /**
         * Factory that allows this ViewModel to be created with the context of the application.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

                TimelineViewModel(app)
            }
        }
    }
}