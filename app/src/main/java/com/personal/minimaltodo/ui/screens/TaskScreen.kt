package com.personal.minimaltodo.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.minimaltodo.data.ScheduleType
import com.personal.minimaltodo.data.TaskEntity
import com.personal.minimaltodo.ui.components.MinimalInputField
import com.personal.minimaltodo.ui.components.SegmentedPillControl
import com.personal.minimaltodo.ui.components.TactileButton
import com.personal.minimaltodo.ui.theme.LocalMinimalPalette
import com.personal.minimaltodo.ui.theme.MotionSpecs
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class AdvancedMode(val label: String) {
    REPEAT_WEEKLY("Repeat Weekly"),
    ONE_TIME("One Time")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskScreen(
    allTasks: List<TaskEntity>,
    onAddTask: (String, ScheduleType, Set<DayOfWeek>, LocalDate?) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val keyboard = LocalSoftwareKeyboardController.current

    var taskName by remember { mutableStateOf("") }
    var advancedExpanded by remember { mutableStateOf(false) }
    var advancedMode by remember { mutableStateOf(AdvancedMode.REPEAT_WEEKLY) }
    var selectedDays by remember { mutableStateOf<Set<DayOfWeek>>(emptySet()) }
    var oneTimeDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (advancedExpanded) 180f else 0f,
        animationSpec = MotionSpecs.snappySpring(),
        label = "advancedChevron"
    )

    fun submitTask() {
        val trimmed = taskName.trim()
        if (trimmed.isEmpty()) return

        val scheduleType = when {
            !advancedExpanded -> ScheduleType.DAILY
            advancedMode == AdvancedMode.REPEAT_WEEKLY && selectedDays.isNotEmpty() -> ScheduleType.WEEKLY
            advancedMode == AdvancedMode.ONE_TIME -> ScheduleType.ONE_TIME
            else -> ScheduleType.DAILY
        }

        onAddTask(
            trimmed,
            scheduleType,
            if (scheduleType == ScheduleType.WEEKLY) selectedDays else emptySet(),
            if (scheduleType == ScheduleType.ONE_TIME) oneTimeDate else null
        )

        taskName = ""
        advancedExpanded = false
        selectedDays = emptySet()
        oneTimeDate = LocalDate.now()
        keyboard?.hide()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Tasks",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Creation card (flat, minimal surface with smooth content-size animation)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .border(1.dp, palette.border, RoundedCornerShape(18.dp))
                .animateContentSize(animationSpec = MotionSpecs.snappySpring())
                .padding(14.dp)
        ) {
            MinimalInputField(
                value = taskName,
                onValueChange = { taskName = it },
                placeholder = "New task...",
                imeAction = ImeAction.Done,
                onImeAction = ::submitTask
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { advancedExpanded = !advancedExpanded }
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Advanced Settings",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textSecondary
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (advancedExpanded) "Collapse" else "Expand",
                        tint = palette.textSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(chevronRotation)
                    )
                }

                TactileButton(
                    onClick = ::submitTask,
                    enabled = taskName.isNotBlank()
                ) {
                    Text(
                        text = "Add Task",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            AnimatedVisibility(
                visible = advancedExpanded,
                enter = fadeIn(tween(180)) + expandVertically(MotionSpecs.snappySpring()),
                exit = fadeOut(tween(140)) + shrinkVertically(MotionSpecs.snappySpring())
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    SegmentedPillControl(
                        items = AdvancedMode.entries,
                        selectedItem = advancedMode,
                        onSelect = { advancedMode = it },
                        labelProvider = { it.label },
                        height = 40.dp,
                        cornerRadius = 12.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedContent(
                        targetState = advancedMode,
                        transitionSpec = {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                        },
                        label = "advancedModeContent"
                    ) { mode ->
                        when (mode) {
                            AdvancedMode.REPEAT_WEEKLY -> {
                                Column {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        DayOfWeek.entries.forEach { day ->
                                            val isSelected = day in selectedDays
                                            DayChip(
                                                day = day,
                                                selected = isSelected,
                                                onClick = {
                                                    selectedDays = if (isSelected) {
                                                        selectedDays - day
                                                    } else {
                                                        selectedDays + day
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (selectedDays.isEmpty()) {
                                            "No days selected — defaults to every day"
                                        } else {
                                            "Repeats weekly on selected days"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            AdvancedMode.ONE_TIME -> {
                                val formattedDate = remember(oneTimeDate) {
                                    oneTimeDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(palette.elevatedCard)
                                        .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                                        .clickable { showDatePicker = true }
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Scheduled date",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = palette.textPrimary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = "Pick date",
                                        tint = palette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "All Tasks",
            style = MaterialTheme.typography.labelLarge,
            color = palette.textSecondary,
            modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)
        )

        if (allTasks.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = "Add your first task above",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    items = allTasks,
                    key = { it.id }
                ) { task ->
                    ManageTaskRow(
                        task = task,
                        onDelete = { onDeleteTask(task) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(220),
                            placementSpec = MotionSpecs.snappySpring(),
                            fadeOutSpec = tween(180)
                        )
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(oneTimeDate) {
            oneTimeDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            oneTimeDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", color = palette.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = palette.elevatedCard
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = palette.elevatedCard,
                    selectedDayContainerColor = palette.accent,
                    selectedDayContentColor = palette.onAccent,
                    todayDateBorderColor = palette.accent,
                    todayContentColor = palette.textPrimary
                )
            )
        }
    }
}

@Composable
private fun DayChip(
    day: DayOfWeek,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = MotionSpecs.snappySpring(),
        label = "dayChipScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) palette.accent else palette.elevatedCard,
        animationSpec = tween(160),
        label = "dayChipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) palette.onAccent else palette.textSecondary,
        animationSpec = tween(160),
        label = "dayChipText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) palette.accent else palette.border,
        animationSpec = tween(160),
        label = "dayChipBorder"
    )

    val shortLabel = when (day) {
        DayOfWeek.MONDAY -> "M"
        DayOfWeek.TUESDAY -> "T"
        DayOfWeek.WEDNESDAY -> "W"
        DayOfWeek.THURSDAY -> "T"
        DayOfWeek.FRIDAY -> "F"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "S"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(38.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = shortLabel,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = textColor
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ManageTaskRow(
    task: TaskEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val deleteInteraction = remember { MutableInteractionSource() }
    val isDeletePressed by deleteInteraction.collectIsPressedAsState()
    val deleteScale by animateFloatAsState(
        targetValue = if (isDeletePressed) 0.88f else 1f,
        animationSpec = MotionSpecs.snappySpring(),
        label = "deleteBtnScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.elevatedCard)
            .border(1.dp, palette.border.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onDelete
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = task.scheduleSummary(),
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(36.dp)
                .graphicsLayer {
                    scaleX = deleteScale
                    scaleY = deleteScale
                }
                .clip(CircleShape)
                .background(palette.surface)
                .clickable(
                    interactionSource = deleteInteraction,
                    indication = null,
                    onClick = onDelete
                )
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete task",
                tint = palette.textSecondary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
