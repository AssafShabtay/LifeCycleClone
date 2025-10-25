package com.yourcompany.lifecycleclone.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourcompany.lifecycleclone.premium.PremiumRepository
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.premium.BackupManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import android.net.Uri

/**
 * ViewModel backing the settings screen.  It exposes premium status and provides methods
 * to toggle premium as well as export and import data backups.  Backup operations are
 * asynchronous and should be called from the UI with proper user interaction (e.g. using
 * Storage Access Framework to pick a destination URI).
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val premiumRepository = PremiumRepository(application)
    private val backupManager = BackupManager(application, AppDatabase.getInstance(application))

    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun togglePremium() {
        premiumRepository.togglePremium()
    }

    fun exportBackup(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = backupManager.exportTo(uri)
            onComplete(success)
        }
    }

    fun importBackup(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = backupManager.importFrom(uri)
            onComplete(success)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application

                SettingsViewModel(app)
            }
        }
    }
}