package com.yourcompany.lifecycleclone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourcompany.lifecycleclone.core.db.VisitWithPlace
import com.yourcompany.lifecycleclone.ui.components.DailyDonutChart
import com.yourcompany.lifecycleclone.ui.components.DailyDonutSlice
import com.yourcompany.lifecycleclone.ui.components.TimelineGraph
import com.yourcompany.lifecycleclone.ui.components.TimelineSegment
import com.yourcompany.lifecycleclone.ui.util.getColorForCategory
import java.util.Locale

/**
 * Timeline screen showing a list of visits for today. It observes the visit data from a
 * [TimelineViewModel] and displays each entry with its label, start and end times.
 */
@Composable
fun HomeTimelineScreen(navController: NavController) {
    val viewModel: TimelineViewModel = viewModel(factory = TimelineViewModel.Factory)
    val visits = viewModel.todayVisits.collectAsState().value

    val now = java.time.ZonedDateTime.now()
    val startOfDay = now.toLocalDate().atStartOfDay(now.zone).toInstant().toEpochMilli()
    val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).toInstant().toEpochMilli()
    val nowMillis = System.currentTimeMillis()

    val segments = visits.sortedBy { it.startTime }.map { visit ->
        val segStart = maxOf(visit.startTime, startOfDay)
        val segEnd: Long? = visit.endTime?.let { endTime -> minOf(endTime, endOfDay) }
        TimelineSegment(
            startTime = segStart,
            endTime = segEnd,
            color = getColorForCategory(visit.placeCategory),
            label = visit.placeCategory
        )
    }

    val donutSlices = visits.sortedBy { it.startTime }.mapNotNull { visit ->
        val sliceStart = maxOf(visit.startTime, startOfDay)
        val rawEnd = visit.endTime ?: nowMillis
        val sliceEnd = minOf(rawEnd, endOfDay)
        val durationMillis = (sliceEnd - sliceStart).coerceAtLeast(0L)
        val durationMinutes = ((durationMillis + 59_999L) / 60_000L).toInt()
        if (durationMinutes <= 0) {
            null
        } else {
            val startMinutes = ((sliceStart - startOfDay).coerceAtLeast(0L) / 60_000L).toInt()
            val isOngoing = visit.endTime == null && sliceEnd > sliceStart
            DailyDonutSlice(
                label = visit.placeCategory,
                startMinuteOfDay = startMinutes,
                durationMinutes = durationMinutes,
                color = getColorForCategory(visit.placeCategory),
                strokeWidthFractionOverride = if (isOngoing) 0.22f else null
            )
        }
    }
    val totalTrackedMinutes = donutSlices.sumOf { it.durationMinutes }
    val trackedSummary = if (totalTrackedMinutes == 0) {
        "0h 00m"
    } else {
        val hours = totalTrackedMinutes / 60
        val minutes = totalTrackedMinutes % 60
        String.format(Locale.getDefault(), "%dh %02dm", hours, minutes)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Today's Timeline",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        DailyDonutChart(
            slices = donutSlices,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp),
            size = 260.dp,
            defaultStrokeWidthFraction = 0.2f,
            defaultGapDegrees = 1.2f,
            tickHours = listOf(0, 6, 12, 18),
            centerContent = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tracked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = trackedSummary,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        TimelineGraph(
            segments = segments,
            startOfDay = startOfDay,
            endOfDay = endOfDay,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(bottom = 8.dp)
        )

        if (visits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No visits recorded yet. Go explore and come back later!")
            }
        } else {
            val categoryDurations: List<Pair<String, Long>> = run {
                val totals = mutableMapOf<String, Long>()
                visits.forEach { v ->
                    val end = minOf(v.endTime ?: nowMillis, endOfDay)
                    val duration = (end - v.startTime).coerceAtLeast(0L)
                    totals.merge(v.placeCategory, duration) { acc, value -> acc + value }
                }
                totals.entries.sortedByDescending { it.value }.map { it.key to it.value }
            }

            if (categoryDurations.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Today's Summary",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    categoryDurations.forEach { (category, millis) ->
                        val hours = millis / 3_600_000
                        val minutes = (millis % 3_600_000) / 60_000
                        val color = getColorForCategory(category)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .size(12.dp)
                                    .padding(end = 4.dp)
                            ) {
                                drawRect(color = color)
                            }
                            val summary = String.format(
                                Locale.getDefault(),
                                "%s - %dh %02dm",
                                category,
                                hours,
                                minutes
                            )
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

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
    Column(
        modifier = Modifier
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
            text = "$start - $end",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Category: ${visit.placeCategory}",
            style = MaterialTheme.typography.labelSmall
        )
    }
}