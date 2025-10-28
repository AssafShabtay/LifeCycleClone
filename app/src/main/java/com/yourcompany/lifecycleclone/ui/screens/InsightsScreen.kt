package com.yourcompany.lifecycleclone.ui.screens

import DonutChart
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
import com.yourcompany.lifecycleclone.ui.components.ChartSegment
import com.yourcompany.lifecycleclone.ui.util.getColorForCategory

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
            // Prepare chart segments with colours
            val chartSegments = breakdown.map { segment ->
                val color = getColorForCategory(segment.category)
                ChartSegment(label = segment.category, fraction = segment.percentOfInterval, color = color)
            }
            // Render donut chart and legend
            DonutChart(
                segments = chartSegments,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                size = 200.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            breakdown.forEach { segment ->
                val percent = segment.percentOfInterval * 100f
                val colorBoxSize = 12.dp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Canvas(modifier = Modifier
                        .size(colorBoxSize)
                        .padding(end = 4.dp)) {
                        drawRect(color = getColorForCategory(segment.category))
                    }
                    Text(
                        text = String.format("%s – %.1f%% (%.1f h)", segment.category, percent, segment.totalMinutes / 60f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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

/**
 * Provides a deterministic colour for each activity category.  Known categories map to
 * Material style colours for a more harmonious palette.  Unknown categories are hashed
 * into the colour space to provide a stable but unique colour.
 */
/*
 * Colour helper moved to [com.yourcompany.lifecycleclone.ui.util.getColorForCategory].
 * This private copy has been removed to avoid duplication.  See that file for details.
 */