package com.yourcompany.lifecycleclone.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.premium.BackupManager
import com.yourcompany.lifecycleclone.premium.PremiumRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel backing the settings screen.
 *
 * Exposes:
 * - isPremium: current premium status
 * - togglePremium(): flip premium on/off (for testing / dev)
 * - exportBackup() / importBackup(): run backup I/O to/from a Uri
 *
 * Backup operations should be triggered from UI after SAF gives you a Uri.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val premiumRepository = PremiumRepository(application)
    private val backupManager =
        BackupManager(application, AppDatabase.getInstance(application))

    // Mirror the repository flow into a StateFlow scoped to the VM.
    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

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
                // IMPORTANT: use the correct APPLICATION_KEY
                val app = this[
                    ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
                ] as Application
                SettingsViewModel(app)
            }
        }
    }
}
