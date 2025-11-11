package com.yourcompany.lifecycleclone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourcompany.lifecycleclone.premium.PremiumRepository
import com.yourcompany.lifecycleclone.premium.BackupManager
import com.yourcompany.lifecycleclone.ui.screens.SettingsViewModel

/**
 * Settings screen providing controls for privacy, backup and tracking. Automatic visit
 * logging runs continuously once permissions are granted. Additional settings (backup, premium)
 * can be added here in future updates.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val isPremium = viewModel.isPremium.collectAsState().value
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Automatic tracking is active whenever required permissions are granted.",
            style = MaterialTheme.typography.bodyMedium
        )
        // Premium subscription toggle
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isPremium) "Premium status: Active" else "Premium status: Free",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = { viewModel.togglePremium() }) {
            Text(if (isPremium) "Cancel Premium" else "Purchase Premium")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Data backup not implemented in this demo.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

