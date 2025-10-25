package com.yourcompany.lifecycleclone.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.yourcompany.lifecycleclone.core.db.VisitWithPlace

/**
 * Timeline screen showing a list of visits for today.  It observes the visit data from a
 * [TimelineViewModel] and displays each entry with its label, start and end times.
 */
@Composable
fun HomeTimelineScreen(navController: NavController) {
    val context = LocalContext.current
    // Obtain the ViewModel with a factory to pass the application context.
    val viewModel: TimelineViewModel = viewModel(factory = TimelineViewModel.Factory)
    val visits = viewModel.todayVisits.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Today’s Timeline",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (visits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No visits recorded yet. Go explore and come back later!")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visits) { visit ->
                    VisitRow(visit)
                }
            }
        }
    }
}

@Composable
private fun VisitRow(visit: VisitWithPlace) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
        Text(text = visit.placeLabel, style = MaterialTheme.typography.titleSmall)
        val start = java.time.Instant.ofEpochMilli(visit.startTime)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime().toString()
        val end = visit.endTime?.let {
            java.time.Instant.ofEpochMilli(it)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalTime().toString()
        } ?: "..."
        Text(
            text = "$start – $end",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Category: ${visit.placeCategory}",
            style = MaterialTheme.typography.labelSmall
        )
    }
}