package com.personal.minimaltodo.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.minimaltodo.ui.components.AnimatedCheckbox
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
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val dateLabel = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }
    val greeting = remember(userName) {
        val trimmed = userName.trim()
        if (trimmed.isNotEmpty()) "Hi, $trimmed" else "Today"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top bar with top-left profile/settings icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfileIconButton(onClick = onOpenSettings)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (tasks.isEmpty()) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    items = tasks,
                    key = { it.task.id }
                ) { item ->
                    HomeTaskRow(
                        item = item,
                        onToggle = { onToggleTask(item.task.id, item.isCompleted) },
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
}

@Composable
private fun HomeTaskRow(
    item: HomeTaskItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, palette.border.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
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
