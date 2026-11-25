package com.personal.minimaltodo.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.personal.minimaltodo.ui.components.AnimatedCheckbox
import com.personal.minimaltodo.ui.haptics.LocalHapticEngine
import com.personal.minimaltodo.ui.theme.LocalMinimalPalette
import com.personal.minimaltodo.ui.theme.MotionSpecs
import com.personal.minimaltodo.viewmodel.HomeTaskItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    userName: String,
    today: LocalDate,
    tasks: List<HomeTaskItem>,
    onToggleTask: (Long, Boolean) -> Unit,
    onReorderTasks: (List<Long>) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val haptic = LocalHapticEngine.current
    val latestHaptic by rememberUpdatedState(haptic)
    val listState = rememberLazyListState()
    val latestOnReorder by rememberUpdatedState(onReorderTasks)

    val dateLabel = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    }
    val greeting = remember(userName) {
        val trimmed = userName.trim()
        if (trimmed.isNotEmpty()) "Hi, $trimmed" else "Today"
    }

    val localTasks = remember { mutableStateListOf<HomeTaskItem>() }
    var draggedTaskId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(tasks, draggedTaskId) {
        if (draggedTaskId == null) {
            localTasks.clear()
            localTasks.addAll(tasks)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfileIconButton(
                onClick = {
                    haptic.tick()
                    onOpenSettings()
                }
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (localTasks.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = "No tasks today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary
                )
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { startOffset ->
                                val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                    startOffset.y.toInt() in info.offset..(info.offset + info.size)
                                }
                                val id = hit?.key as? Long
                                if (id != null) {
                                    draggedTaskId = id
                                    dragOffsetY = 0f
                                    latestHaptic.pickup()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val activeId = draggedTaskId ?: return@detectDragGesturesAfterLongPress
                                dragOffsetY += dragAmount.y

                                val currentIndex = localTasks.indexOfFirst { it.task.id == activeId }
                                if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                val currentInfo = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == activeId }
                                    ?: return@detectDragGesturesAfterLongPress

                                val draggedCenterY = currentInfo.offset + currentInfo.size / 2f + dragOffsetY
                                val targetInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                    info.key != activeId &&
                                        draggedCenterY in info.offset.toFloat()..(info.offset + info.size).toFloat()
                                }

                                if (targetInfo != null) {
                                    val targetIndex = targetInfo.index
                                    if (targetIndex in localTasks.indices &&
                                        localTasks[targetIndex].isCompleted == localTasks[currentIndex].isCompleted
                                    ) {
                                        val moved = localTasks.removeAt(currentIndex)
                                        localTasks.add(targetIndex, moved)
                                        dragOffsetY -= (targetInfo.offset - currentInfo.offset)
                                        latestHaptic.slotSwap()
                                    }
                                }
                            },
                            onDragEnd = {
                                if (draggedTaskId != null) {
                                    latestHaptic.drop()
                                    latestOnReorder(localTasks.map { it.task.id })
                                }
                                draggedTaskId = null
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                if (draggedTaskId != null) {
                                    latestOnReorder(localTasks.map { it.task.id })
                                }
                                draggedTaskId = null
                                dragOffsetY = 0f
                            }
                        )
                    }
            ) {
                items(
                    items = localTasks,
                    key = { it.task.id }
                ) { item ->
                    val isDragging = item.task.id == draggedTaskId
                    HomeTaskRow(
                        item = item,
                        isDragging = isDragging,
                        dragTranslationY = if (isDragging) dragOffsetY else 0f,
                        onToggle = {
                            if (draggedTaskId == null) {
                                if (item.isCompleted) haptic.uncheck() else haptic.confirm()
                                onToggleTask(item.task.id, item.isCompleted)
                            }
                        },
                        modifier = if (isDragging) {
                            Modifier.zIndex(1f)
                        } else {
                            Modifier.animateItem(
                                fadeInSpec = tween(220),
                                placementSpec = MotionSpecs.snappySpring(),
                                fadeOutSpec = tween(180)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTaskRow(
    item: HomeTaskItem,
    isDragging: Boolean,
    dragTranslationY: Float,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.03f
            isPressed -> 0.98f
            else -> 1f
        },
        animationSpec = MotionSpecs.snappySpring(),
        label = "homeRowScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (item.isCompleted) 0.52f else 1f,
        animationSpec = tween(220),
        label = "homeRowAlpha"
    )
    val cardBg by animateColorAsState(
        targetValue = if (item.isCompleted) palette.surface.copy(alpha = 0.7f) else palette.elevatedCard,
        animationSpec = tween(220),
        label = "homeRowBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDragging) palette.accent else palette.border.copy(alpha = 0.75f),
        animationSpec = tween(160),
        label = "homeRowBorder"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragTranslationY
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(if (isDragging) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        AnimatedCheckbox(
            checked = item.isCompleted,
            onCheckedChange = { onToggle() }
        )

        Text(
            text = item.task.name,
            style = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = palette.textPrimary.copy(alpha = contentAlpha),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ProfileIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = MotionSpecs.snappySpring(),
        label = "profileBtnScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(palette.surface)
            .border(1.dp, palette.border, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = "Profile and Settings",
            tint = palette.textPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}
