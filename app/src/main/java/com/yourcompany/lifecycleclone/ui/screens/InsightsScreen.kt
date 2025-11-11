package com.yourcompany.lifecycleclone.ui.screens
import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.yourcompany.lifecycleclone.insights.CategoryBreakdown
import com.yourcompany.lifecycleclone.insights.SleepCorrelation
import com.yourcompany.lifecycleclone.ui.util.getColorForCategory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin




@Composable
fun InsightsScreen(navController: NavController) {
    val dailyViewModel: DailyInsightsViewModel = viewModel(factory = DailyInsightsViewModel.Factory)
    val dailyState by dailyViewModel.uiState.collectAsState()

    val weeklyViewModel: InsightsViewModel = viewModel(factory = InsightsViewModel.Factory)
    val weeklyBreakdown = weeklyViewModel.weeklyBreakdown.collectAsState().value
    val correlations = weeklyViewModel.sleepCorrelations.collectAsState().value

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Daily Insights",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            DailyInsightsContent(
                state = dailyState,
                onDateSelected = dailyViewModel::onDateSelected,
                onMonthChanged = dailyViewModel::onDisplayedMonthChanged
            )
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            WeeklySummary(weeklyBreakdown)
            Spacer(modifier = Modifier.height(24.dp))
            SleepCorrelationsSection(correlations)
            Spacer(modifier = Modifier.height(24.dp))
            HowItWorksSection()
        }
    }
}


@Composable
private fun DailyInsightsContent(
    state: DailyInsightsUiState,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit
) {
    val context = LocalContext.current
    var coloredSlots by remember(state.daySlots) {
        mutableStateOf(applySavedColors(context, state.daySlots))
    }
    var selectedSlot by remember(state.selectedDate) { mutableStateOf<ActivityTimeSlot?>(null) }
    var showCustomization by remember(state.selectedDate) { mutableStateOf(false) }

    LaunchedEffect(state.daySlots) {
        coloredSlots = applySavedColors(context, state.daySlots)
    }

    CalendarDateSelector(
        selectedDate = state.selectedDate,
        displayedMonth = state.displayedMonth,
        monthActivityData = state.monthActivityMap,
        onDateSelected = onDateSelected,
        onMonthChanged = onMonthChanged
    )

    Spacer(modifier = Modifier.height(24.dp))

    if (state.isDayLoading) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(modifier = Modifier.width(160.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Loading day activity?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    if (coloredSlots.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No recordings for this day.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Text(
        text = "Tap a segment to customise its colour",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp)
    )

    Spacer(modifier = Modifier.height(12.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedPieChart(
            data = coloredSlots,
            modifier = Modifier.fillMaxSize(),
            onSegmentClick = { slot ->
                selectedSlot = slot
                showCustomization = true
            }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    ActivityRecordingsList(slots = coloredSlots)

    Spacer(modifier = Modifier.height(16.dp))

    QuickStatsCard(coloredSlots)

    if (showCustomization && selectedSlot != null) {
        SlotCustomizationDialog(
            slot = selectedSlot!!,
            onDismiss = { showCustomization = false },
            onColorSelected = { newColor ->
                val target = selectedSlot!!
                saveSlotColorPreference(context, target, newColor)
                val targetKey = slotPreferenceKey(target)
                coloredSlots = coloredSlots.map { current ->
                    if (slotPreferenceKey(current) == targetKey) current.copy(color = newColor) else current
                }
                selectedSlot = target.copy(color = newColor)
            }
        )
    }
}@Composable
private fun CalendarDateSelector(
    selectedDate: LocalDate,
    displayedMonth: YearMonth,
    monthActivityData: Map<Int, List<ActivityTimeSlot>>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val headerFormatter = remember { DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.getDefault()) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedDate.format(headerFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (expanded) {
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onMonthChanged(displayedMonth.minusMonths(1)) }) {
                        Icon(
                            imageVector = Icons.Filled.ExpandLess,
                            contentDescription = "Previous month",
                            modifier = Modifier.rotate(-90f)
                        )
                    }

                    Text(
                        text = displayedMonth.format(monthFormatter),
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(onClick = { onMonthChanged(displayedMonth.plusMonths(1)) }) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = "Next month",
                            modifier = Modifier.rotate(-90f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                CalendarGrid(
                    displayedMonth = displayedMonth,
                    selectedDate = selectedDate,
                    activityByDay = monthActivityData,
                    onDateSelected = { date ->
                        onDateSelected(date)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    activityByDay: Map<Int, List<ActivityTimeSlot>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val firstOfMonth = displayedMonth.atDay(1)
    val startDayOfWeek = firstOfMonth.dayOfWeek.value % 7
    val daysInMonth = displayedMonth.lengthOfMonth()

    val weeks = mutableListOf<List<Int>>()
    var currentWeek = MutableList(7) { 0 }
    var dayCounter = 1

    for (index in startDayOfWeek until 7) {
        if (dayCounter <= daysInMonth) {
            currentWeek[index] = dayCounter
            dayCounter++
        }
    }
    weeks += currentWeek.toList()

    while (dayCounter <= daysInMonth) {
        val week = MutableList(7) { 0 }
        for (index in 0 until 7) {
            if (dayCounter <= daysInMonth) {
                week[index] = dayCounter
                dayCounter++
            }
        }
        weeks += week
    }

    weeks.forEach { week ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            week.forEach { day ->
                if (day == 0) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    val dayDate = displayedMonth.atDay(day)
                    CalendarDay(
                        day = day,
                        isSelected = dayDate == selectedDate,
                        activityData = activityByDay[day]?.let { applySavedColors(context, it) },
                        onClick = { onDateSelected(dayDate) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
    }
};@Composable
private fun CalendarDay(
    day: Int,
    isSelected: Boolean,
    activityData: List<ActivityTimeSlot>?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 2.dp else 0.5.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
        ) {
            if (!activityData.isNullOrEmpty()) {
                MiniPieChart(data = activityData, modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@Composable
private fun MiniPieChart(
    data: List<ActivityTimeSlot>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val radius = canvasSize / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = Color.LightGray.copy(alpha = 0.1f),
            radius = radius,
            center = center
        )

        if (data.isNotEmpty()) {
            val dayMillis = 24 * 60 * 60 * 1000L
            data.forEach { slot ->
                val dayStartMillis = slot.dayStart.time
                val dayEndMillis = dayStartMillis + dayMillis
                val clampedStart = slot.startTime.time.coerceIn(dayStartMillis, dayEndMillis)
                val clampedEnd = slot.endTime.time.coerceIn(dayStartMillis, dayEndMillis)

                var startMinutes = ((clampedStart - dayStartMillis).toFloat() / 60000f)
                var endMinutes = ((clampedEnd - dayStartMillis).toFloat() / 60000f)
                if (endMinutes < startMinutes) endMinutes += 24f * 60f

                val sweepAngle = ((endMinutes - startMinutes) / (24f * 60f)) * 360f
                if (sweepAngle < 1f) return@forEach
                val startAngle = (startMinutes / (24f * 60f)) * 360f - 90f

                drawArc(
                    color = slot.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = center - Offset(radius, radius),
                    size = Size(radius * 2, radius * 2)
                )
            }

            drawCircle(
                color = Color.White,
                radius = radius * 0.3f,
                center = center
            )
        }
    }
}

@Composable
private fun AnimatedPieChart(
    data: List<ActivityTimeSlot>,
    modifier: Modifier = Modifier,
    onSegmentClick: (ActivityTimeSlot) -> Unit = {}
) {
    var animationProgress by remember { mutableStateOf(0f) }

    val textPaint = remember {
        android.graphics.Paint().apply {
            textSize = 18f
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )
            isAntiAlias = true
        }
    }

    data class SegmentBounds(
        val slot: ActivityTimeSlot,
        val startAngle: Float,
        val sweepAngle: Float
    )

    data class SegmentIconInfo(
        val slot: ActivityTimeSlot,
        val center: Offset,
        val alpha: Float,
        val sizeDp: Dp,
        val sizePx: Float
    )

    val segmentBounds = remember { mutableStateListOf<SegmentBounds>() }
    val segmentIcons = remember { mutableStateListOf<SegmentIconInfo>() }

    val iconSize = 20.dp
    val density = LocalDensity.current
    val iconSizePx = with(density) { iconSize.toPx() }

    LaunchedEffect(data) {
        animationProgress = 0f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animationProgress = value
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = remember(backgroundColor) {
        val luminance = 0.299 * backgroundColor.red +
            0.587 * backgroundColor.green +
            0.114 * backgroundColor.blue
        if (luminance > 0.5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }

    Box(
        modifier = modifier.pointerInput(data) {
            detectTapGestures { offset ->
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val distance = kotlin.math.sqrt(
                    (offset.x - centerX) * (offset.x - centerX) +
                        (offset.y - centerY) * (offset.y - centerY)
                )

                val radius = min(size.width, size.height) / 2f * 0.85f
                val innerRadius = radius * 0.55f

                if (distance in innerRadius..radius) {
                    var clickAngle = kotlin.math.atan2(
                        offset.x - centerX,
                        -(offset.y - centerY)
                    ) * 180 / kotlin.math.PI

                    if (clickAngle < 0) clickAngle += 360

                    segmentBounds.forEach { bounds ->
                        val startAngle = bounds.startAngle
                        val endAngle = bounds.startAngle + bounds.sweepAngle

                        val inSegment = if (endAngle > startAngle) {
                            clickAngle >= startAngle && clickAngle <= endAngle
                        } else {
                            clickAngle >= startAngle || clickAngle <= endAngle
                        }

                        if (inSegment) {
                            onSegmentClick(bounds.slot)
                            return@detectTapGestures
                        }
                    }
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2f * 0.85f
            val innerRadius = radius * 0.55f
            val center = Offset(size.width / 2f, size.height / 2f)

            segmentBounds.clear()
            segmentIcons.clear()

            val totalMinutes = if (data.isEmpty()) 0L else data.sumOf { it.durationMillis } / (1000 * 60)
            val totalHours = totalMinutes / 60
            val remainingMinutes = totalMinutes % 60

            val strokeWidth = radius - innerRadius
            val middleRadius = (radius + innerRadius) / 2f

            val millisPerDay = 24 * 60 * 60 * 1000L
            val minRenderableAngle = 0.5f
            val minAngleForIcon = 10f
            val minAngleGeneral = 4f
            val iconDisplayThreshold = 7.5f

            data class BaseSegment(
                val slot: ActivityTimeSlot,
                val startAngle: Float,
                val baseAngle: Float
            )

            data class VisualSegment(
                val slot: ActivityTimeSlot,
                val startAngle: Float,
                val displayAngle: Float,
                val originalAngle: Float
            )

            val baseSegments = data
                .sortedBy { it.startTime }
                .mapNotNull { slot ->
                    val dayStartMillis = slot.dayStart.time
                    val dayEndMillis = dayStartMillis + millisPerDay

                    val clampedStartMillis = slot.startTime.time.coerceIn(dayStartMillis, dayEndMillis)
                    val clampedEndMillis = slot.endTime.time.coerceIn(dayStartMillis, dayEndMillis)

                    var startMinutes = ((clampedStartMillis - dayStartMillis).toFloat() / 60000f)
                    var endMinutes = ((clampedEndMillis - dayStartMillis).toFloat() / 60000f)
                    if (endMinutes < startMinutes) endMinutes += 24f * 60f

                    val rawAngle = ((endMinutes - startMinutes) / (24f * 60f)) * 360f
                    val normalizedAngle = when {
                        slot.isActive -> max(rawAngle, minAngleGeneral)
                        else -> rawAngle
                    }
                    if (normalizedAngle < minRenderableAngle && !slot.isActive) {
                        null
                    } else {
                        val startAngle = (startMinutes / (24f * 60f)) * 360f - 90f
                        BaseSegment(slot, startAngle, normalizedAngle)
                    }
                }

            val visualSegments = mutableListOf<VisualSegment>()
            if (baseSegments.isNotEmpty()) {
                val boostedSegments = MutableList(baseSegments.size) { false }
                val targetAngles = MutableList(baseSegments.size) { index ->
                    val slot = baseSegments[index].slot
                    val baseAngle = baseSegments[index].baseAngle
                    if (slot.durationMillis >= 15 * 60 * 1000L && baseAngle < minAngleForIcon) {
                        boostedSegments[index] = true
                        minAngleForIcon
                    } else {
                        baseAngle
                    }
                }

                var totalAngle = targetAngles.sum()
                if (totalAngle > 360f) {
                    var excess = totalAngle - 360f

                    fun reduce(indices: List<Int>, floor: Float) {
                        if (indices.isEmpty() || excess <= 0f) return
                        var available = 0f
                        indices.forEach { idx ->
                            available += (targetAngles[idx] - floor).coerceAtLeast(0f)
                        }
                        if (available <= 0f) return
                        val reduction = excess.coerceAtMost(available)
                        indices.forEach { idx ->
                            val adjustable = (targetAngles[idx] - floor).coerceAtLeast(0f)
                            if (adjustable > 0f) {
                                val share = (adjustable / available) * reduction
                                targetAngles[idx] -= share
                            }
                        }
                        excess -= reduction
                    }

                    val adjustable = baseSegments.indices.filter { !boostedSegments[it] && targetAngles[it] > minAngleGeneral }
                    reduce(adjustable, minAngleGeneral)

                    val allCandidates = baseSegments.indices.filter { targetAngles[it] > minAngleGeneral }
                    reduce(allCandidates, minAngleGeneral)

                    val boosted = baseSegments.indices.filter { boostedSegments[it] && targetAngles[it] > minAngleForIcon }
                    reduce(boosted, minAngleForIcon)
                }

                var currentAngle = baseSegments.first().startAngle
                baseSegments.indices.forEach { index ->
                    val base = baseSegments[index]
                    if (index > 0) {
                        val desired = base.startAngle
                        if (desired > currentAngle) {
                            currentAngle = desired
                        }
                    }
                    val displayAngle = targetAngles[index]
                    visualSegments.add(
                        VisualSegment(
                            slot = base.slot,
                            startAngle = currentAngle,
                            displayAngle = displayAngle,
                            originalAngle = base.baseAngle
                        )
                    )
                    currentAngle += displayAngle
                }
            }

            var angleRemaining = 360f * animationProgress
            val gapPerSideDegrees = 0f
            visualSegments.forEach { segment ->
                val segmentDisplay = segment.displayAngle
                val visibleFraction = when {
                    angleRemaining <= 0f -> 0f
                    angleRemaining >= segmentDisplay -> 1f
                    segmentDisplay > 0f -> angleRemaining / segmentDisplay
                    else -> 0f
                }
                angleRemaining = (angleRemaining - segmentDisplay).coerceAtLeast(0f)

                val animatedSweep = segment.displayAngle * visibleFraction
                val targetGap = if (segment.displayAngle > gapPerSideDegrees * 2f) gapPerSideDegrees else 0f
                val animatedGap = if (targetGap > 0f && animatedSweep > targetGap * 2f) targetGap else 0f
                val trimmedSweep = (animatedSweep - animatedGap * 2f).coerceAtLeast(0f)

                if (trimmedSweep > 0.5f) {
                    val arcStartAngle = segment.startAngle + animatedGap

                    drawArc(
                        color = segment.slot.color,
                        startAngle = arcStartAngle,
                        sweepAngle = trimmedSweep,
                        useCenter = false,
                        topLeft = center - Offset(middleRadius, middleRadius),
                        size = Size(middleRadius * 2, middleRadius * 2),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Butt
                        )
                    )

                    val detectionGap = targetGap
                    val trimmedDisplayAngle = (segment.displayAngle - detectionGap * 2f).coerceAtLeast(0f)
                    if (trimmedDisplayAngle > 0f) {
                        segmentBounds.add(
                            SegmentBounds(
                                slot = segment.slot,
                                startAngle = segment.startAngle + 90 + detectionGap,
                                sweepAngle = trimmedDisplayAngle
                            )
                        )
                    }

                    if (
                        segment.slot.durationMillis >= 15 * 60 * 1000L &&
                        visibleFraction > 0.9f &&
                        trimmedDisplayAngle >= iconDisplayThreshold
                    ) {
                        val midAngle = segment.startAngle + detectionGap + (trimmedDisplayAngle / 2f)
                        val midAngleRad = Math.toRadians(midAngle.toDouble())

                        val textRadiusForSegment = innerRadius + strokeWidth * 0.45f
                        val iconRadiusPreferred = innerRadius + strokeWidth * 0.75f
                        val angleRad = Math.toRadians(trimmedDisplayAngle.toDouble())
                        val halfAngleSin = sin(angleRad / 2.0).toFloat()
                        val chordText = 2f * textRadiusForSegment * halfAngleSin
                        val chordIcon = 2f * iconRadiusPreferred * halfAngleSin

                        val dayStartMillisForSlot = segment.slot.dayStart.time
                        val totalDurationCandidate = segment.slot.originalEndTime.time - segment.slot.originalStartTime.time
                        val totalDurationMillis = max(segment.slot.durationMillis, max(totalDurationCandidate, 0L))
                        val durationMillisForLabel = if (segment.slot.originalStartTime.time < dayStartMillisForSlot) {
                            totalDurationMillis
                        } else {
                            segment.slot.durationMillis
                        }
                        val durationMinutes = durationMillisForLabel / (1000 * 60)
                        val durationText = when {
                            durationMinutes >= 60 -> "${durationMinutes / 60}h${if (durationMinutes % 60 > 0) "${durationMinutes % 60}m" else ""}"
                            else -> "${durationMinutes}m"
                        }

                        val textPaintSegment = android.graphics.Paint(textPaint)
                        var textWidth = textPaintSegment.measureText(durationText)
                        val maxTextWidth = (chordText - 10f).coerceAtLeast(0f)
                        if (maxTextWidth > 0f && textWidth > maxTextWidth) {
                            val scale = (maxTextWidth / textWidth).coerceIn(0.5f, 1f)
                            textPaintSegment.textSize *= scale
                            textWidth = textPaintSegment.measureText(durationText)
                        }
                        val metrics = textPaintSegment.fontMetrics
                        val textYOffset = (metrics.ascent + metrics.descent) / 2f
                        val minTextRadius = innerRadius + textPaintSegment.textSize
                        val maxTextRadius = radius - textPaintSegment.textSize

                        if (maxTextWidth > 0f && textWidth <= chordText + 1f && minTextRadius < maxTextRadius) {
                            val safeTextRadius = textRadiusForSegment.coerceIn(minTextRadius, maxTextRadius)
                            val textX = center.x + (safeTextRadius * cos(midAngleRad)).toFloat()
                            val textY = center.y + (safeTextRadius * sin(midAngleRad)).toFloat() - textYOffset
                            drawContext.canvas.nativeCanvas.drawText(
                                durationText,
                                textX,
                                textY,
                                textPaintSegment
                            )
                        }

                        val iconAlpha = ((animationProgress - 0.6f) / 0.4f).coerceIn(0f, 1f)
                        val availableIcon = chordIcon - 8f
                        if (iconAlpha > 0f && visibleFraction > 0.9f && availableIcon > 4f) {
                            var iconSizePxAdjusted = iconSizePx.coerceAtMost(availableIcon)
                            val minIconPx = iconSizePx * 0.4f
                            if (iconSizePxAdjusted < minIconPx) {
                                iconSizePxAdjusted = if (availableIcon <= minIconPx) availableIcon else minIconPx
                            }

                            val maxIconRadius = radius - iconSizePxAdjusted / 2f
                            val minIconRadius = innerRadius + iconSizePxAdjusted / 2f
                            val iconRadius = iconRadiusPreferred.coerceIn(minIconRadius, maxIconRadius)

                            val iconX = center.x + (iconRadius * cos(midAngleRad)).toFloat()
                            val iconY = center.y + (iconRadius * sin(midAngleRad)).toFloat()
                            val iconSizeDpAdjusted = with(density) { iconSizePxAdjusted.toDp() }

                            segmentIcons.add(
                                SegmentIconInfo(
                                    slot = segment.slot,
                                    center = Offset(iconX, iconY),
                                    alpha = iconAlpha,
                                    sizeDp = iconSizeDpAdjusted,
                                    sizePx = iconSizePxAdjusted
                                )
                            )
                        }
                    }
                }
            }

            val centerTextPaint = android.graphics.Paint().apply {
                textSize = 42f
                color = textColor
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD
                )
                isAntiAlias = true
            }

            drawContext.canvas.nativeCanvas.drawText(
                if (totalHours > 0 || remainingMinutes > 0) {
                    "${totalHours}h ${remainingMinutes}m"
                } else {
                    "No data"
                },
                center.x,
                center.y - 5,
                centerTextPaint
            )

            drawContext.canvas.nativeCanvas.drawText(
                "tracked",
                center.x,
                center.y + 30,
                android.graphics.Paint().apply {
                    textSize = 24f
                    color = textColor
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }

        segmentIcons.forEach { iconInfo ->
            val iconVector = getIconVectorForActivity(iconInfo.slot)

            Icon(
                imageVector = iconVector,
                contentDescription = iconInfo.slot.activityType,
                modifier = Modifier
                    .size(iconInfo.sizeDp)
                    .offset {
                        IntOffset(
                            (iconInfo.center.x - iconInfo.sizePx / 2f).roundToInt(),
                            (iconInfo.center.y - iconInfo.sizePx / 2f).roundToInt()
                        )
                    }
                    .alpha(iconInfo.alpha),
                tint = Color.White
            )
        }
    }
}


@Composable
private fun SlotCustomizationDialog(
    slot: ActivityTimeSlot,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    var previewColor by remember(slot) { mutableStateOf(slot.color) }
    val palette = remember {
        listOf(
            Color(0xFF2196F3),
            Color(0xFF4CAF50),
            Color(0xFFFF9800),
            Color(0xFFE91E63),
            Color(0xFFFFC107),
            Color(0xFF673AB7),
            Color(0xFF009688),
            Color(0xFF795548),
            Color(0xFF607D8B)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
        title = {
            Column {
                Text(
                    text = slot.placeName ?: slot.activityType,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${timeFormatter.format(slot.startTime)} - ${timeFormatter.format(slot.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Pick a colour",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    palette.chunked(4).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { colourOption ->
                                val isSelected = colourOption == previewColor
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 40.dp else 32.dp)
                                        .clip(CircleShape)
                                        .background(colourOption)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            previewColor = colourOption
                                            onColorSelected(colourOption)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

private const val ACTIVITY_COLOR_PREFS = "activity_color_preferences"
private const val ACTIVITY_PREF_PREFIX = "activity_slot:"

private fun saveSlotColorPreference(context: Context, slot: ActivityTimeSlot, colour: Color) {
    val preferences = context.getSharedPreferences(ACTIVITY_COLOR_PREFS, Context.MODE_PRIVATE)
    preferences.edit()
        .putInt(slotPreferenceKey(slot), colour.toArgb())
        .apply()
}

private fun applySavedColors(context: Context, slots: List<ActivityTimeSlot>): List<ActivityTimeSlot> {
    if (slots.isEmpty()) return slots
    val preferences = context.getSharedPreferences(ACTIVITY_COLOR_PREFS, Context.MODE_PRIVATE)
    return slots.map { slot ->
        val key = slotPreferenceKey(slot)
        if (preferences.contains(key)) {
            val stored = preferences.getInt(key, slot.color.toArgb())
            val colourLong = stored.toLong() and 0xFFFFFFFFL
            slot.copy(color = Color(colourLong))
        } else {
            slot
        }
    }
}

private fun slotPreferenceKey(slot: ActivityTimeSlot): String {
    val base = slot.placeName?.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault())
        ?: slot.activityType.lowercase(Locale.getDefault())
    val category = slot.placeCategory?.lowercase(Locale.getDefault()) ?: ""
    return "$ACTIVITY_PREF_PREFIX$base|$category"
}

@Composable
private fun ActivityRecordingsList(slots: List<ActivityTimeSlot>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activity Records",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
            slots.forEachIndexed { index, slot ->
                ActivityRecordingRow(slot = slot, formatter = formatter)
                if (index < slots.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ActivityRecordingRow(slot: ActivityTimeSlot, formatter: SimpleDateFormat) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(slot.color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slot.placeName ?: slot.activityType,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${formatter.format(slot.startTime)} - ${formatter.format(slot.endTime)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Duration: ${formatDuration(slot.durationMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            slot.placeCategory?.let { category ->
                Text(
                    text = "Category: $category",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickStatsCard(activityTimeSlots: List<ActivityTimeSlot>) {
    if (activityTimeSlots.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Stats",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val grouped = activityTimeSlots.groupBy { it.placeCategory ?: it.activityType }
            val longest = grouped.maxByOrNull { entry -> entry.value.sumOf { it.durationMillis } }
            longest?.let { entry ->
                InfoRow("Most Time", entry.key)
            }
            val activeMillis = activityTimeSlots
                .filterNot { (it.placeCategory ?: it.activityType).equals("Still", ignoreCase = true) }
                .sumOf { it.durationMillis }
            InfoRow("Total Active", formatDuration(activeMillis))
            InfoRow("Segments", activityTimeSlots.size.toString())
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> String.format(Locale.getDefault(), "%dh %02dm", hours, minutes % 60)
        minutes > 0 -> String.format(Locale.getDefault(), "%dm %02ds", minutes, totalSeconds % 60)
        else -> String.format(Locale.getDefault(), "%ds", totalSeconds)
    }
}

private fun getIconVectorForActivity(slot: ActivityTimeSlot): ImageVector {
    val category = slot.placeCategory?.lowercase(Locale.getDefault()) ?: slot.activityType.lowercase(Locale.getDefault())
    return when {
        "walk" in category -> Icons.AutoMirrored.Filled.DirectionsWalk
        "run" in category -> Icons.AutoMirrored.Filled.DirectionsRun
        "bike" in category || "cycle" in category -> Icons.AutoMirrored.Filled.DirectionsBike
        "drive" in category || "car" in category -> Icons.Filled.DirectionsCar
        "home" in category || "still" in category -> Icons.Filled.Home
        else -> Icons.AutoMirrored.Filled.Help
    }
}

@Composable
private fun WeeklySummary(breakdown: List<CategoryBreakdown>) {
    Text(
        text = "Weekly Breakdown",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    if (breakdown.isEmpty()) {
        Text(
            text = "No data yet. Tracking will populate your insights.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    breakdown.forEach { segment ->
        val percent = segment.percentOfInterval * 100f
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Canvas(modifier = Modifier
                .size(12.dp)
                .padding(end = 4.dp)) {
                drawRect(color = getColorForCategory(segment.category))
            }
            Text(
                text = String.format(Locale.getDefault(), "%s - %.1f%% (%.1f h)", segment.category, percent, segment.totalMinutes / 60f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SleepCorrelationsSection(correlations: List<SleepCorrelation>) {
    Text(
        text = "Sleep Correlations",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    if (correlations.isEmpty()) {
        Text(
            text = "No sleep data yet. Connect to Health and grant permissions to view correlations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    correlations.forEach { corr ->
        val percent = corr.fraction * 100f
        Text(
            text = String.format(Locale.getDefault(), "%s - %d sleeps (%.1f%%)", corr.category, corr.count, percent),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}




@Composable
private fun HowItWorksSection() {
    Text(
        text = "How Tracking Works",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            howItWorksSections.forEachIndexed { index, section ->
                FeatureOverviewSection(section)
                if (index < howItWorksSections.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureOverviewSection(section: FeatureOverviewEntry) {
    Text(
        text = section.title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    section.bullets.forEach { point ->
        FeatureBullet(point)
    }
}

@Composable
private fun FeatureBullet(text: String) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "-",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

private data class FeatureOverviewEntry(
    val title: String,
    val bullets: List<String>
)

private val howItWorksSections = listOf(
    FeatureOverviewEntry(
        title = "Tracking Lifecycle",
        bullets = listOf(
            "The app won't collect anything until fine/background location, activity recognition, and notification permissions are granted; PermissionsActivity walks the user through requesting them and closes once they're all allowed (PermissionsActivity.kt:67, PermissionsActivity.kt:116).",
            "With permissions in place the main screen boots straight into tracking: it starts the foreground LocationService, registers receivers, and keeps the UI updated (MainActivity.kt:1127, MainActivity.kt:157, MainActivity.kt:975)."
        )
    ),
    FeatureOverviewEntry(
        title = "Activity Classification",
        bullets = listOf(
            "Google Activity Recognition transitions drive state: entering STILL creates a still session immediately, while entering any movement type (vehicle, bicycle, running, walking) opens a movement session; exits finalize whichever session was active (LocationService.kt:231, LocationService.kt:247-LocationService.kt:289).",
            "Still sessions continuously update their duration and last known coordinates, then resolve nearby Places data once they end (LocationService.kt:292-LocationService.kt:355).",
            "Movement sessions gather buffered GPS points; if the trip lasted under 15 minutes on foot or never left a 100 m radius, they are discarded and reclassified as a still stay instead (LocationService.kt:68, LocationService.kt:399-LocationService.kt:433)."
        )
    ),
    FeatureOverviewEntry(
        title = "Sleep Detection",
        bullets = listOf(
            "Long still periods between 9 PM and 6 AM are treated as potential sleep; only stretches lasting 2-12 hours pass the filters and get recorded as sleep sessions, with overlaps avoided via a mutex guard (SleepDetectionManager.kt:11-SleepDetectionManager.kt:46)."
        )
    ),
    FeatureOverviewEntry(
        title = "Geofenced Location Slots",
        bullets = listOf(
            "Users can define named \"slots\" (home, work, etc.); each becomes a geofence that logs ENTER/EXIT/DWELL events, keeps active visit timers, and optionally fires notifications when you come and go (LocationSlotsActivity.kt:20-LocationSlotsActivity.kt:78, GeofenceManager.kt:201-GeofenceManager.kt:307).",
            "The main screen listens for these broadcasts to show where you currently are inside the card at the top (MainActivity.kt:60, MainActivity.kt:1002)."
        )
    ),
    FeatureOverviewEntry(
        title = "Insights & Aids",
        bullets = listOf(
            "The overview tab surfaces your live activity state, current geofence slot, quick database access, and a helper note spelling out the practical rules: tracking auto-starts with permissions, still locations are automatic, movement captures start/end points, 100 m movements become \"still\", and sample data can be generated for the charts (MainActivity.kt:370-MainActivity.kt:468).",
            "The trends tab compiles the last week of data into activity totals, daily bars, and location highlights once enough history exists (MainActivity.kt:474-MainActivity.kt:666)."
        )
    )
)



