package com.yourcompany.lifecycleclone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourcompany.lifecycleclone.ui.components.DonutChart

/**
 * Insights screen summarising the user's time distribution across categories for the current week.
 * Uses a [DonutChart] to visualise the breakdown.  If no data is present, a message is shown.
 */
@Composable
fun InsightsScreen(navController: NavController) {
    val viewModel: InsightsViewModel = viewModel(factory = InsightsViewModel.Factory)
    val breakdown = viewModel.weeklyBreakdown.collectAsState().value
    val correlations = viewModel.sleepCorrelations.collectAsState().value
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Weekly Insights",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (breakdown.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No data yet. Tracking will populate your insights.")
            }
        } else {
            // Render donut chart and legend
            DonutChart(
                segments = breakdown,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                diameter = 200.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            breakdown.forEach { segment ->
                val percent = segment.percentOfInterval * 100f
                Text(
                    text = String.format("%s – %.1f%% (%.1f h)", segment.category, percent, segment.totalMinutes / 60f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Display sleep correlations if available
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Sleep Correlations",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (correlations.isEmpty()) {
                Text(
                    text = "No sleep data yet. Connect to Health and grant permissions to view correlations.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                correlations.forEach { corr ->
                    val percentCorr = corr.fraction * 100f
                    Text(
                        text = String.format("%s – %d sleeps (%.1f%%)", corr.category, corr.count, percentCorr),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}