package com.personal.minimaltodo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.personal.minimaltodo.ui.theme.LocalMinimalPalette
import com.personal.minimaltodo.ui.theme.MotionSpecs
import com.personal.minimaltodo.viewmodel.DayProgress
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun ReportScreen(
    today: LocalDate,
    todayProgress: DayProgress,
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    monthProgressMap: Map<LocalDate, DayProgress>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top-left profile/settings icon (same as Home)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfileIconButton(onClick = onOpenSettings)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Report",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Today's animated Pie Chart Card
        TodayPieChartCard(progress = todayProgress)

        Spacer(modifier = Modifier.height(16.dp))

        // Historical Progress Calendar Card
        CalendarHistoryCard(
            today = today,
            displayedMonth = displayedMonth,
            selectedDate = selectedDate,
            monthProgressMap = monthProgressMap,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectDate = onSelectDate
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Selected day breakdown
        val selectedProgress = monthProgressMap[selectedDate] ?: DayProgress(selectedDate, emptyList())
        SelectedDayBreakdownCard(progress = selectedProgress)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TodayPieChartCard(progress: DayProgress) {
    val palette = LocalMinimalPalette.current
    val targetSweep = if (progress.totalCount == 0) 0f else 360f * progress.completionRatio
    val sweepAnim = remember { Animatable(0f) }

    LaunchedEffect(targetSweep) {
        sweepAnim.animateTo(
            targetValue = targetSweep,
            animationSpec = MotionSpecs.gentleSpring()
        )
    }

    val percentage = if (progress.totalCount == 0) {
        "—"
    } else {
        "${(progress.completionRatio * 100).toInt()}%"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.elevatedCard)
            .border(1.dp, palette.border.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Text(
            text = "Today's Progress",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(164.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 20.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(inset, inset)

                // Pending / track ring
                drawArc(
                    color = palette.surface,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Completed animated arc
                val sweep = sweepAnim.value.coerceIn(0f, 360f)
                if (sweep > 0.5f) {
                    drawArc(
                        color = palette.accent,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = percentage,
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (progress.totalCount == 0) {
                        "No tasks"
                    } else {
                        "${progress.completedCount} of ${progress.totalCount}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(
                dotColor = palette.accent,
                label = "Completed (${progress.completedCount})"
            )
            LegendItem(
                dotColor = palette.surface,
                borderColor = palette.border,
                label = "Pending (${progress.pendingCount})"
            )
        }
    }
}

@Composable
private fun LegendItem(
    dotColor: androidx.compose.ui.graphics.Color,
    label: String,
    borderColor: androidx.compose.ui.graphics.Color = dotColor
) {
    val palette = LocalMinimalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
                .border(1.dp, borderColor, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textSecondary
        )
    }
}

@Composable
private fun CalendarHistoryCard(
    today: LocalDate,
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    monthProgressMap: Map<LocalDate, DayProgress>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val palette = LocalMinimalPalette.current
    val monthTitle = remember(displayedMonth) {
        displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    val weekdayLabels = remember { listOf("M", "T", "W", "T", "F", "S", "S") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.elevatedCard)
            .border(1.dp, palette.border.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.surface)
                    .clickable(onClick = onPreviousMonth)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = "Previous month",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textPrimary
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.surface)
                    .clickable(onClick = onNextMonth)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Next month",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = displayedMonth,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
            label = "calendarMonthGrid"
        ) { month ->
            val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
            val totalDays = month.lengthOfMonth()
            val totalCells = ((firstDayOffset + totalDays + 6) / 7) * 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (rowStart in 0 until totalCells step 7) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = rowStart + col
                            val dayNum = cellIndex - firstDayOffset + 1
                            if (dayNum in 1..totalDays) {
                                val date = month.atDay(dayNum)
                                val dayProgress = monthProgressMap[date]
                                val isFuture = date.isAfter(today)
                                val isSelected = date == selectedDate
                                val isToday = date == today

                                CalendarDayCell(
                                    dayNumber = dayNum,
                                    ratio = if (isFuture || dayProgress == null || dayProgress.totalCount == 0) {
                                        null
                                    } else {
                                        dayProgress.completionRatio
                                    },
                                    isSelected = isSelected,
                                    isToday = isToday,
                                    isFuture = isFuture,
                                    onClick = { onSelectDate(date) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    ratio: Float?,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current

    val targetFill = when {
        ratio == null -> palette.surface.copy(alpha = 0.4f)
        ratio <= 0f -> palette.surface
        else -> palette.accent.copy(alpha = 0.22f + 0.78f * ratio)
    }
    val fill by animateColorAsState(
        targetValue = targetFill,
        animationSpec = tween(200),
        label = "calCellFill"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> palette.textPrimary
            isToday -> palette.accent
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = tween(160),
        label = "calCellBorder"
    )
    val textColor = when {
        isFuture -> palette.disabled
        ratio != null && ratio >= 0.65f -> palette.onAccent
        else -> palette.textPrimary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(fill)
            .border(
                width = if (isSelected || isToday) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = textColor
        )
    }
}

@Composable
private fun SelectedDayBreakdownCard(progress: DayProgress) {
    val palette = LocalMinimalPalette.current
    val dateTitle = remember(progress.date) {
        progress.date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(18.dp))
            .animateContentSize(MotionSpecs.snappySpring())
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = dateTitle,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            if (progress.totalCount > 0) {
                Text(
                    text = "${progress.completedCount}/${progress.totalCount} done",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (progress.scheduledTasks.isEmpty()) {
            Text(
                text = "No tasks scheduled on this day",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                progress.scheduledTasks.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (item.isCompleted) palette.accent else palette.border
                                )
                        )
                        Text(
                            text = item.task.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (item.isCompleted) {
                                    TextDecoration.LineThrough
                                } else {
                                    TextDecoration.None
                                }
                            ),
                            color = if (item.isCompleted) {
                                palette.textSecondary
                            } else {
                                palette.textPrimary
                            }
                        )
                    }
                }
            }
        }
    }
}
