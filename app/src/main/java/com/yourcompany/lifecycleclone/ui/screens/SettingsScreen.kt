package com.yourcompany.lifecycleclone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yourcompany.lifecycleclone.settings.TrackingController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourcompany.lifecycleclone.premium.PremiumRepository
import com.yourcompany.lifecycleclone.premium.BackupManager
import com.yourcompany.lifecycleclone.ui.screens.SettingsViewModel

/**
 * Settings screen providing controls for privacy, backup and tracking.  Users can start or
 * stop automatic tracking via the buttons below.  Additional settings (backup, premium)
 * can be added here in future updates.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
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
        Button(onClick = { TrackingController.startTracking(context) }) {
            Text("Start Tracking")
        }
        Button(onClick = { TrackingController.stopTracking(context) }) {
            Text("Stop Tracking")
        }
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