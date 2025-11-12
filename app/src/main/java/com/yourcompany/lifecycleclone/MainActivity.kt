package com.yourcompany.lifecycleclone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourcompany.lifecycleclone.core.db.AppDatabase
import com.yourcompany.lifecycleclone.tracking.service.LocationService
import com.yourcompany.lifecycleclone.ui.Donut24hChartView
import com.yourcompany.lifecycleclone.ui.TimelineRow
import com.yourcompany.lifecycleclone.ui.TimelineSliceAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var actionButton: Button
    private lateinit var donutChart: Donut24hChartView
    private lateinit var chartLegendText: TextView
    private lateinit var timelineRecycler: RecyclerView
    private val sliceAdapter = TimelineSliceAdapter()

    private val basePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        requestBackgroundLocationIfNeeded()
        updateUi()
        refreshTimelineCard()
        if (hasAllTrackingPermissions()) {
            startTrackingService()
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        updateUi()
        refreshTimelineCard()
        if (hasAllTrackingPermissions()) {
            startTrackingService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        actionButton = findViewById(R.id.actionButton)
        donutChart = findViewById(R.id.databaseChart)
        chartLegendText = findViewById(R.id.chartLegendText)
        timelineRecycler = findViewById(R.id.timelineRecycler)
        timelineRecycler.layoutManager = LinearLayoutManager(this)
        timelineRecycler.adapter = sliceAdapter

        actionButton.setOnClickListener {
            if (hasAllTrackingPermissions()) {
                startTrackingService()
            } else {
                requestRuntimePermissions()
            }
        }

        updateUi()
        refreshTimelineCard()
        if (hasAllTrackingPermissions()) {
            startTrackingService()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
        refreshTimelineCard()
        if (hasAllTrackingPermissions()) {
            startTrackingService()
        }
    }

    private fun requestRuntimePermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionsToRequest += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            permissionsToRequest += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (needsActivityRecognitionPermission() &&
            !hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        ) {
            permissionsToRequest += Manifest.permission.ACTIVITY_RECOGNITION
        }
        if (needsNotificationPermission() &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            permissionsToRequest += Manifest.permission.POST_NOTIFICATIONS
        }

        if (permissionsToRequest.isNotEmpty()) {
            basePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            requestBackgroundLocationIfNeeded()
        }
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (!needsBackgroundLocationPermission()) return
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            return
        }
        if (!hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun updateUi() {
        if (!::statusText.isInitialized || !::actionButton.isInitialized) {
            return
        }
        if (hasAllTrackingPermissions()) {
            statusText.text = getString(R.string.status_tracking_ready)
            actionButton.text = getString(R.string.action_start_tracking)
        } else {
            statusText.text = getString(R.string.status_permissions_required)
            actionButton.text = getString(R.string.action_grant_permissions)
        }
    }

    private fun refreshTimelineCard() {
        if (!::donutChart.isInitialized || !::chartLegendText.isInitialized) return
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { buildChartData() }
            }
            if (result.isFailure) {
                donutChart.setSegments(emptyList())
                chartLegendText.text = getString(R.string.database_summary_placeholder)
                sliceAdapter.submitList(emptyList())
                return@launch
            }
            val data = result.getOrNull()
            if (data == null || data.segments.isEmpty()) {
                donutChart.setSegments(emptyList())
                chartLegendText.text = getString(R.string.database_summary_placeholder)
                sliceAdapter.submitList(emptyList())
            } else {
                donutChart.setSegments(data.segments)
                chartLegendText.text = data.legend
                sliceAdapter.submitList(data.rows)
            }
        }
    }

    private suspend fun buildChartData(): ChartData {
        val now = System.currentTimeMillis()
        val windowStart = now - DAY_MILLIS
        val db = AppDatabase.getInstance(applicationContext)
        val visits = db.visitDao().getVisitsInRange(windowStart, now)
        val tracked = visits.mapNotNull { visit ->
            val start = max(windowStart, visit.startTime)
            val rawEnd = visit.endTime ?: now
            val end = min(now, rawEnd)
            if (end <= start) {
                null
            } else {
                TimelineSlice(
                    startMillis = start,
                    endMillis = end,
                    color = visit.placeColor.toInt(),
                    label = visit.placeLabel
                )
            }
        }.sortedBy { it.startMillis }
        val slices = mergeWithGaps(tracked, windowStart, now)
        val segments = slices.map { slice ->
            val startAngle = ((slice.startMillis - windowStart).toFloat() / DAY_MILLIS) * FULL_CIRCLE_DEGREES
            val sweepAngle = (slice.durationMillis.toFloat() / DAY_MILLIS) * FULL_CIRCLE_DEGREES
            Donut24hChartView.Segment(startAngle, sweepAngle.coerceAtLeast(ANGLE_EPSILON), slice.color)
        }
        val legend = buildLegendText(slices)
        val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
        val rows = slices.map { slice ->
            val detail = buildString {
                append(timeFormat.format(Date(slice.startMillis)))
                append(" - ")
                append(timeFormat.format(Date(slice.endMillis)))
                append(" (")
                append(formatDuration(slice.durationMillis / MINUTE_MILLIS))
                append(')')
            }
            TimelineRow(label = slice.label, detail = detail, color = slice.color)
        }
        return ChartData(segments, legend, rows)
    }

    private fun mergeWithGaps(
        tracked: List<TimelineSlice>,
        windowStart: Long,
        windowEnd: Long
    ): List<TimelineSlice> {
        val placeholderLabel = getString(R.string.legend_untracked_label)
        if (tracked.isEmpty()) {
            return listOf(
                TimelineSlice(windowStart, windowEnd, GAP_COLOR, placeholderLabel)
            )
        }
        val slices = mutableListOf<TimelineSlice>()
        var cursor = windowStart
        tracked.forEach { slice ->
            if (slice.startMillis > cursor) {
                slices += TimelineSlice(cursor, slice.startMillis, GAP_COLOR, placeholderLabel)
            }
            slices += slice
            cursor = slice.endMillis
        }
        if (cursor < windowEnd) {
            slices += TimelineSlice(cursor, windowEnd, GAP_COLOR, placeholderLabel)
        }
        return slices
    }

    private fun buildLegendText(slices: List<TimelineSlice>): CharSequence {
        val builder = SpannableStringBuilder()
        val entries = slices.groupBy { it.label }.map { (label, parts) ->
            LegendEntry(
                label = label,
                color = parts.first().color,
                durationMinutes = parts.sumOf { it.durationMillis } / MINUTE_MILLIS
            )
        }.sortedByDescending { it.durationMinutes }

        if (entries.isEmpty()) {
            builder.append(getString(R.string.database_summary_placeholder))
            return builder
        }

        entries.forEachIndexed { index, entry ->
            val bulletStart = builder.length
            builder.append("\u2022 ")
            builder.setSpan(
                ForegroundColorSpan(entry.color),
                bulletStart,
                bulletStart + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            val labelStart = builder.length
            builder.append(entry.label)
            builder.setSpan(
                StyleSpan(Typeface.BOLD),
                labelStart,
                builder.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(" - ")
            builder.append(formatDuration(entry.durationMinutes))
            builder.appendLine()
            if (index == entries.lastIndex) {
                builder.delete(builder.length - 1, builder.length)
            }
        }
        return builder
    }

    private fun formatDuration(minutes: Long): String {
        if (minutes <= 0) return "<1m"
        val hoursPart = minutes / 60
        val minutesPart = minutes % 60
        return when {
            hoursPart > 0 && minutesPart > 0 -> String.format(Locale.getDefault(), "%dh %02dm", hoursPart, minutesPart)
            hoursPart > 0 -> String.format(Locale.getDefault(), "%dh", hoursPart)
            else -> String.format(Locale.getDefault(), "%dm", minutesPart)
        }
    }

    private fun hasAllTrackingPermissions(): Boolean {
        val foregroundOk = hasForegroundLocationPermission()
        val backgroundOk = !needsBackgroundLocationPermission() ||
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val activityOk = !needsActivityRecognitionPermission() ||
            hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
        val notificationsOk = !needsNotificationPermission() ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        return foregroundOk && backgroundOk && activityOk && notificationsOk
    }

    private fun hasForegroundLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun needsBackgroundLocationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun needsActivityRecognitionPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    private fun startTrackingService() {
        val intent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private data class TimelineSlice(
        val startMillis: Long,
        val endMillis: Long,
        val color: Int,
        val label: String
    ) {
        val durationMillis: Long get() = endMillis - startMillis
    }

    private data class LegendEntry(
        val label: String,
        val color: Int,
        val durationMinutes: Long
    )

    private data class ChartData(
        val segments: List<Donut24hChartView.Segment>,
        val legend: CharSequence,
        val rows: List<TimelineRow>
    )

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val MINUTE_MILLIS = 60 * 1000L
        private const val FULL_CIRCLE_DEGREES = 360f
        private const val ANGLE_EPSILON = 0.5f
        private const val GAP_COLOR = 0xFFB0B0B0.toInt()
    }
}
