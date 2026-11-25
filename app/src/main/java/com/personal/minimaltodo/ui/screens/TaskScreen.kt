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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.minimaltodo.data.ScheduleType
import com.personal.minimaltodo.data.TaskEntity
import com.personal.minimaltodo.ui.components.MinimalInputField
import com.personal.minimaltodo.ui.components.SegmentedPillControl
import com.personal.minimaltodo.ui.components.TactileButton
import com.personal.minimaltodo.ui.haptics.LocalHapticEngine
import com.personal.minimaltodo.ui.theme.LocalMinimalPalette
import com.personal.minimaltodo.ui.theme.MotionSpecs
import com.personal.minimaltodo.viewmodel.ManageTaskItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class AdvancedMode(val label: String) {
    REPEAT_WEEKLY("Repeat Weekly"),
    ONE_TIME("One Time")
}

private enum class EditScheduleChoice(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    ONE_TIME("Dates")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    taskItems: List<ManageTaskItem>,
    onAddTask: (String, ScheduleType, Set<DayOfWeek>, List<LocalDate>) -> Unit,
    onUpdateTask: (TaskEntity, String, ScheduleType, Set<DayOfWeek>, List<LocalDate>) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val haptic = LocalHapticEngine.current
    val keyboard = LocalSoftwareKeyboardController.current

    var taskName by remember { mutableStateOf("") }
    var advancedExpanded by remember { mutableStateOf(false) }
    var advancedMode by remember { mutableStateOf(AdvancedMode.REPEAT_WEEKLY) }
    var selectedDays by remember { mutableStateOf<Set<DayOfWeek>>(emptySet()) }
    var oneTimeDates by remember { mutableStateOf(listOf(LocalDate.now())) }

    var datePickerTargetIndex by remember { mutableStateOf<Int?>(null) }
    var showCreateDatePicker by remember { mutableStateOf(false) }

    var taskPendingDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var taskPendingEditConfirm by remember { mutableStateOf<TaskEntity?>(null) }
    var taskBeingEdited by remember { mutableStateOf<TaskEntity?>(null) }

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

        haptic.confirm()
        onAddTask(
            trimmed,
            scheduleType,
            if (scheduleType == ScheduleType.WEEKLY) selectedDays else emptySet(),
            if (scheduleType == ScheduleType.ONE_TIME) oneTimeDates else emptyList()
        )

        taskName = ""
        advancedExpanded = false
        selectedDays = emptySet()
        oneTimeDates = listOf(LocalDate.now())
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

        // Creation card
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
                        ) {
                            haptic.tick()
                            advancedExpanded = !advancedExpanded
                        }
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
                        onSelect = { mode ->
                            if (mode != advancedMode) {
                                haptic.tick()
                                advancedMode = mode
                            }
                        },
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
                                    WeeklyDaySelector(
                                        selectedDays = selectedDays,
                                        onToggleDay = { day ->
                                            haptic.tick()
                                            selectedDays = if (day in selectedDays) {
                                                selectedDays - day
                                            } else {
                                                selectedDays + day
                                            }
                                        }
                                    )
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
                                MultiDateSelector(
                                    dates = oneTimeDates,
                                    onEditDateAt = { idx ->
                                        haptic.tick()
                                        datePickerTargetIndex = idx
                                        showCreateDatePicker = true
                                    },
                                    onRemoveDateAt = { idx ->
                                        if (oneTimeDates.size > 1) {
                                            haptic.tick()
                                            oneTimeDates = oneTimeDates.filterIndexed { i, _ -> i != idx }
                                        }
                                    },
                                    onAddAnotherDate = {
                                        haptic.tick()
                                        datePickerTargetIndex = null
                                        showCreateDatePicker = true
                                    }
                                )
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

        if (taskItems.isEmpty()) {
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
                    items = taskItems,
                    key = { it.task.id }
                ) { item ->
                    ManageTaskRow(
                        item = item,
                        onDeleteClick = {
                            haptic.tick()
                            taskPendingDelete = item.task
                        },
                        onLongHoldEdit = { taskPendingEditConfirm = item.task },
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

    if (showCreateDatePicker) {
        val initialDate = datePickerTargetIndex?.let { oneTimeDates.getOrNull(it) }
            ?: oneTimeDates.lastOrNull()?.plusDays(1)
            ?: LocalDate.now()

        MinimalDatePickerDialog(
            initialDate = initialDate,
            onDismiss = { showCreateDatePicker = false },
            onDateSelected = { picked ->
                haptic.tick()
                val idx = datePickerTargetIndex
                oneTimeDates = if (idx != null && idx in oneTimeDates.indices) {
                    oneTimeDates.toMutableList().apply { this[idx] = picked }.distinct().sorted()
                } else {
                    (oneTimeDates + picked).distinct().sorted()
                }
                showCreateDatePicker = false
            }
        )
    }

    // Delete confirmation popup (no Undo snackbar)
    taskPendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskPendingDelete = null },
            containerColor = palette.elevatedCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = "Delete task",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Do you want to delete \"${task.name}\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.delete()
                        onDeleteTask(task)
                        taskPendingDelete = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = palette.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { taskPendingDelete = null }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }

    // 1.5s hold Edit confirmation dialog
    taskPendingEditConfirm?.let { task ->
        AlertDialog(
            onDismissRequest = { taskPendingEditConfirm = null },
            containerColor = palette.elevatedCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = "Edit task",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Do you want to edit \"${task.name}\"?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.tick()
                        taskBeingEdited = task
                        taskPendingEditConfirm = null
                    }
                ) {
                    Text(
                        text = "Yes",
                        color = palette.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { taskPendingEditConfirm = null }) {
                    Text("No", color = palette.textSecondary)
                }
            }
        )
    }

    taskBeingEdited?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { taskBeingEdited = null },
            onSave = { newName, scheduleType, days, dates ->
                haptic.confirm()
                onUpdateTask(task, newName, scheduleType, days, dates)
                taskBeingEdited = null
            }
        )
    }
}

@Composable
private fun WeeklyDaySelector(
    selectedDays: Set<DayOfWeek>,
    onToggleDay: (DayOfWeek) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        DayOfWeek.entries.forEach { day ->
            DayChip(
                day = day,
                selected = day in selectedDays,
                onClick = { onToggleDay(day) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MultiDateSelector(
    dates: List<LocalDate>,
    onEditDateAt: (Int) -> Unit,
    onRemoveDateAt: (Int) -> Unit,
    onAddAnotherDate: () -> Unit
) {
    val palette = LocalMinimalPalette.current
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        dates.forEachIndexed { index, date ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.elevatedCard)
                    .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                    .clickable { onEditDateAt(index) }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (dates.size == 1) "Scheduled date" else "Date ${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = date.format(formatter),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = palette.textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Change date",
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    if (dates.size > 1) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(palette.surface)
                                .clickable { onRemoveDateAt(index) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove date",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.elevatedCard.copy(alpha = 0.7f))
                .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                .clickable(onClick = onAddAnotherDate)
                .padding(vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = "Add another date",
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary
            )
        }
    }
}

@Composable
private fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (String, ScheduleType, Set<DayOfWeek>, List<LocalDate>) -> Unit
) {
    val palette = LocalMinimalPalette.current
    val haptic = LocalHapticEngine.current

    var editedName by remember(task.id) { mutableStateOf(task.name) }
    var scheduleChoice by remember(task.id) {
        mutableStateOf(
            when (task.scheduleType) {
                ScheduleType.DAILY -> EditScheduleChoice.DAILY
                ScheduleType.WEEKLY -> EditScheduleChoice.WEEKLY
                ScheduleType.ONE_TIME -> EditScheduleChoice.ONE_TIME
            }
        )
    }
    var editedDays by remember(task.id) { mutableStateOf(task.selectedDays()) }
    var editedDates by remember(task.id) {
        val existing = task.specificDates()
        mutableStateOf(if (existing.isEmpty()) listOf(LocalDate.now()) else existing)
    }
    var showEditDatePicker by remember { mutableStateOf(false) }
    var editDateIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.elevatedCard,
        titleContentColor = palette.textPrimary,
        title = {
            Text(
                text = "Edit Task",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(MotionSpecs.snappySpring())
            ) {
                MinimalInputField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    placeholder = "Task name"
                )

                SegmentedPillControl(
                    items = EditScheduleChoice.entries,
                    selectedItem = scheduleChoice,
                    onSelect = { choice ->
                        if (choice != scheduleChoice) {
                            haptic.tick()
                            scheduleChoice = choice
                        }
                    },
                    labelProvider = { it.label },
                    height = 38.dp,
                    cornerRadius = 11.dp
                )

                when (scheduleChoice) {
                    EditScheduleChoice.DAILY -> {
                        Text(
                            text = "Appears every day on your Home screen.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }

                    EditScheduleChoice.WEEKLY -> {
                        WeeklyDaySelector(
                            selectedDays = editedDays,
                            onToggleDay = { day ->
                                haptic.tick()
                                editedDays = if (day in editedDays) {
                                    editedDays - day
                                } else {
                                    editedDays + day
                                }
                            }
                        )
                    }

                    EditScheduleChoice.ONE_TIME -> {
                        MultiDateSelector(
                            dates = editedDates,
                            onEditDateAt = { idx ->
                                haptic.tick()
                                editDateIndex = idx
                                showEditDatePicker = true
                            },
                            onRemoveDateAt = { idx ->
                                if (editedDates.size > 1) {
                                    haptic.tick()
                                    editedDates = editedDates.filterIndexed { i, _ -> i != idx }
                                }
                            },
                            onAddAnotherDate = {
                                haptic.tick()
                                editDateIndex = null
                                showEditDatePicker = true
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = editedName.isNotBlank(),
                onClick = {
                    val finalType = when (scheduleChoice) {
                        EditScheduleChoice.DAILY -> ScheduleType.DAILY
                        EditScheduleChoice.WEEKLY -> if (editedDays.isEmpty()) ScheduleType.DAILY else ScheduleType.WEEKLY
                        EditScheduleChoice.ONE_TIME -> ScheduleType.ONE_TIME
                    }
                    onSave(
                        editedName.trim(),
                        finalType,
                        if (finalType == ScheduleType.WEEKLY) editedDays else emptySet(),
                        if (finalType == ScheduleType.ONE_TIME) editedDates else emptyList()
                    )
                }
            ) {
                Text(
                    text = "Save",
                    color = if (editedName.isNotBlank()) palette.accent else palette.disabled,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textSecondary)
            }
        }
    )

    if (showEditDatePicker) {
        val initialDate = editDateIndex?.let { editedDates.getOrNull(it) }
            ?: editedDates.lastOrNull()?.plusDays(1)
            ?: LocalDate.now()

        MinimalDatePickerDialog(
            initialDate = initialDate,
            onDismiss = { showEditDatePicker = false },
            onDateSelected = { picked ->
                haptic.tick()
                val idx = editDateIndex
                editedDates = if (idx != null && idx in editedDates.indices) {
                    editedDates.toMutableList().apply { this[idx] = picked }.distinct().sorted()
                } else {
                    (editedDates + picked).distinct().sorted()
                }
                showEditDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val palette = LocalMinimalPalette.current
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        onDateSelected(picked)
                    } ?: onDismiss()
                }
            ) {
                Text("Select", color = palette.accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textSecondary)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = palette.elevatedCard)
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

@Composable
private fun ManageTaskRow(
    item: ManageTaskItem,
    onDeleteClick: () -> Unit,
    onLongHoldEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val haptic = LocalHapticEngine.current
    val latestHaptic by rememberUpdatedState(haptic)
    val latestOnLongHoldEdit by rememberUpdatedState(onLongHoldEdit)
    val scope = rememberCoroutineScope()

    val task = item.task
    val isDoneOneTime = item.isOneTimeCompleted

    val deleteInteraction = remember { MutableInteractionSource() }
    val isDeletePressed by deleteInteraction.collectIsPressedAsState()
    val deleteScale by animateFloatAsState(
        targetValue = if (isDeletePressed) 0.88f else 1f,
        animationSpec = MotionSpecs.snappySpring(),
        label = "deleteBtnScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isDoneOneTime) 0.52f else 1f,
        animationSpec = tween(220),
        label = "manageRowAlpha"
    )
    val cardBg by animateColorAsState(
        targetValue = if (isDoneOneTime) palette.surface.copy(alpha = 0.7f) else palette.elevatedCard,
        animationSpec = tween(220),
        label = "manageRowBg"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, palette.border.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .pointerInput(task.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val slop = viewConfiguration.touchSlop

                    // Fast ramping micro-ticks culminating at 250ms (quarter-second) hold
                    val holdJob = scope.launch {
                        delay(70L)
                        latestHaptic.holdTick(0.35f)
                        delay(60L)
                        latestHaptic.holdTick(0.65f)
                        delay(60L)
                        latestHaptic.holdTick(0.90f)
                        delay(60L)
                        latestHaptic.confirm()
                        latestOnLongHoldEdit()
                    }

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            if ((change.position - down.position).getDistance() > slop) break
                        }
                    } finally {
                        holdJob.cancel()
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (isDoneOneTime) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = palette.textPrimary.copy(alpha = contentAlpha),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = task.scheduleSummary(),
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary.copy(alpha = contentAlpha)
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
                    onClick = onDeleteClick
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
