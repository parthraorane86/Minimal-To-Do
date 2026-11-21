package com.personal.minimaltodo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.minimaltodo.ui.theme.LocalMinimalPalette
import com.personal.minimaltodo.ui.theme.MotionSpecs

/**
 * Dark Mode Switch inspired by Galahhad (https://uiverse.io/Galahhad/strong-squid-82),
 * re-skinned to the app's sage/neutral palette with spring-based physics.
 */
@Composable
fun DarkModeSunMoonSwitch(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val progress by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = MotionSpecs.bouncySpring(),
        label = "darkModeProgress"
    )

    val trackLight = Color(0xFF9EB5A3) // soft sage-sky daylight
    val trackDark = Color(0xFF222420)  // dark surface night
    val trackColor by animateColorAsState(
        targetValue = if (isDark) trackDark else trackLight,
        animationSpec = tween(260),
        label = "switchTrackColor"
    )

    val sunColor = Color(0xFFE7D8A5)
    val moonColor = Color(0xFFEAECE6)
    val craterColor = Color(0xFFA6A69B)
    val cloudFrontColor = Color(0xFFF5F5F0)
    val cloudBackColor = Color(0xFFD5D5CD)
    val starColor = Color(0xFFEAECE6)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = MotionSpecs.snappySpring(),
        label = "switchPressScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .width(74.dp)
            .height(34.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, palette.border, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch
            ) { onToggle(!isDark) }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val clamped = progress.coerceIn(0f, 1f)

            // Back & front clouds sliding smoothly downward on dark mode
            val cloudDropY = h * 0.95f * clamped
            val backClouds = listOf(
                Offset(w * 0.24f, h * 0.88f + cloudDropY) to h * 0.28f,
                Offset(w * 0.48f, h * 0.94f + cloudDropY) to h * 0.24f,
                Offset(w * 0.72f, h * 0.82f + cloudDropY) to h * 0.30f,
                Offset(w * 0.90f, h * 0.65f + cloudDropY) to h * 0.34f
            )
            backClouds.forEach { (center, radius) ->
                drawCircle(
                    color = cloudBackColor.copy(alpha = (1f - clamped).coerceIn(0f, 1f)),
                    radius = radius,
                    center = center
                )
            }

            val frontClouds = listOf(
                Offset(w * 0.36f, h * 1.02f + cloudDropY) to h * 0.28f,
                Offset(w * 0.60f, h * 0.98f + cloudDropY) to h * 0.27f,
                Offset(w * 0.84f, h * 0.90f + cloudDropY) to h * 0.32f
            )
            frontClouds.forEach { (center, radius) ->
                drawCircle(
                    color = cloudFrontColor.copy(alpha = (1f - clamped).coerceIn(0f, 1f)),
                    radius = radius,
                    center = center
                )
            }

            // Minimal stars sliding down from top on dark mode
            val starOffsetY = -h * (1f - clamped)
            val stars = listOf(
                Offset(w * 0.19f, h * 0.32f + starOffsetY) to 2.2.dp.toPx(),
                Offset(w * 0.31f, h * 0.62f + starOffsetY) to 1.6.dp.toPx(),
                Offset(w * 0.42f, h * 0.28f + starOffsetY) to 1.8.dp.toPx(),
                Offset(w * 0.27f, h * 0.44f + starOffsetY) to 1.2.dp.toPx(),
                Offset(w * 0.47f, h * 0.64f + starOffsetY) to 1.4.dp.toPx()
            )
            stars.forEach { (center, radius) ->
                drawCircle(
                    color = starColor.copy(alpha = clamped),
                    radius = radius,
                    center = center
                )
            }

            // Sliding thumb position
            val thumbRadius = h * 0.38f
            val startX = h * 0.5f
            val endX = w - h * 0.5f
            val centerX = startX + (endX - startX) * progress
            val centerY = h * 0.5f

            // Soft concentric translucent halo rings (Galahhad signature detail)
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = thumbRadius * 2.15f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = thumbRadius * 1.65f,
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = thumbRadius * 1.25f,
                center = Offset(centerX, centerY)
            )

            // Sun base + clipped sliding spotted moon overlay
            val orbPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        left = centerX - thumbRadius,
                        top = centerY - thumbRadius,
                        right = centerX + thumbRadius,
                        bottom = centerY + thumbRadius
                    )
                )
            }
            clipPath(orbPath) {
                drawCircle(
                    color = sunColor,
                    radius = thumbRadius,
                    center = Offset(centerX, centerY)
                )

                val moonShiftX = thumbRadius * 2f * (1f - clamped)
                val moonCenterX = centerX + moonShiftX
                drawCircle(
                    color = moonColor,
                    radius = thumbRadius,
                    center = Offset(moonCenterX, centerY)
                )

                // Three subtle moon craters
                drawCircle(
                    color = craterColor.copy(alpha = 0.7f),
                    radius = thumbRadius * 0.28f,
                    center = Offset(moonCenterX - thumbRadius * 0.25f, centerY + thumbRadius * 0.16f)
                )
                drawCircle(
                    color = craterColor.copy(alpha = 0.65f),
                    radius = thumbRadius * 0.16f,
                    center = Offset(moonCenterX + thumbRadius * 0.30f, centerY + thumbRadius * 0.26f)
                )
                drawCircle(
                    color = craterColor.copy(alpha = 0.55f),
                    radius = thumbRadius * 0.13f,
                    center = Offset(moonCenterX + thumbRadius * 0.08f, centerY - thumbRadius * 0.34f)
                )
            }
        }
    }
}

/**
 * Minimal rounded input box inspired by ErzenXz (https://uiverse.io/ErzenXz/horrible-rattlesnake-14),
 * styled with subtle surface contrast, hairline border, and spring focus feedback.
 */
@Composable
fun MinimalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    val palette = LocalMinimalPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) palette.accent else palette.border,
        animationSpec = tween(180),
        label = "inputBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 1.dp,
        animationSpec = MotionSpecs.snappySpring(),
        label = "inputBorderWidth"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isFocused) palette.elevatedCard else palette.elevatedCard.copy(alpha = 0.85f),
        animationSpec = tween(180),
        label = "inputBgColor"
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.textPrimary),
        cursorBrush = SolidColor(palette.accent),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction() },
            onSend = { onImeAction() }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textSecondary
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = palette.textSecondary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onValueChange("") }
                    )
                }
            }
        }
    )
}

/**
 * Recessed pill segmented toggle inspired by Yaya12085 (https://uiverse.io/Yaya12085/rude-mouse-79),
 * with a spring-animated sliding elevated pill indicator.
 */
@Composable
fun <T> SegmentedPillControl(
    items: List<T>,
    selectedItem: T,
    onSelect: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    showAccentDot: Boolean = false
) {
    val palette = LocalMinimalPalette.current
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(cornerRadius))
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / items.size.coerceAtLeast(1)
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = MotionSpecs.snappySpring(),
            label = "segmentOffset"
        )

        // Sliding elevated pill
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(cornerRadius - 3.dp))
                .background(palette.elevatedCard)
                .border(0.5.dp, palette.border.copy(alpha = 0.6f), RoundedCornerShape(cornerRadius - 3.dp))
        )

        Row(modifier = Modifier.matchParentSize()) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) palette.textPrimary else palette.textSecondary,
                    animationSpec = tween(160),
                    label = "segmentTextColor"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(cornerRadius - 3.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) { onSelect(item) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (showAccentDot && isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(palette.accent)
                            )
                        }
                        Text(
                            text = labelProvider(item),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = textColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom animated checkbox with spring bounce and checkmark path draw-in.
 */
@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val fillColor by animateColorAsState(
        targetValue = if (checked) palette.accent else Color.Transparent,
        animationSpec = tween(180),
        label = "checkboxFill"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) palette.accent else palette.border,
        animationSpec = tween(180),
        label = "checkboxBorder"
    )
    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = MotionSpecs.snappySpring(),
        label = "checkDrawProgress"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.88f
            checked -> 1.0f
            else -> 1.0f
        },
        animationSpec = MotionSpecs.bouncySpring(),
        label = "checkboxScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(26.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(fillColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox
            ) { onCheckedChange(!checked) }
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val progress = checkProgress.coerceIn(0f, 1f)
            if (progress > 0.01f) {
                val fullPath = Path().apply {
                    moveTo(size.width * 0.16f, size.height * 0.52f)
                    lineTo(size.width * 0.42f, size.height * 0.78f)
                    lineTo(size.width * 0.86f, size.height * 0.24f)
                }
                val measure = PathMeasure()
                measure.setPath(fullPath, false)
                val drawn = Path()
                measure.getSegment(0f, measure.length * progress, drawn, true)
                drawPath(
                    path = drawn,
                    color = palette.onAccent,
                    style = Stroke(
                        width = 2.3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Simple spring-animated sage pill switch for settings toggles.
 */
@Composable
fun MinimalToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val trackColor by animateColorAsState(
        targetValue = if (checked) palette.accent else palette.surface,
        animationSpec = tween(200),
        label = "toggleTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = MotionSpecs.bouncySpring(),
        label = "toggleThumbOffset"
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, if (checked) palette.accent else palette.border, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch
            ) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) palette.onAccent else palette.elevatedCard)
        )
    }
}

/**
 * Tactile iOS-style press button with 0.96x scale-down spring feedback.
 */
@Composable
fun TactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = LocalMinimalPalette.current.accent,
    contentColor: Color = LocalMinimalPalette.current.onAccent,
    content: @Composable () -> Unit
) {
    val palette = LocalMinimalPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = MotionSpecs.snappySpring(),
        label = "buttonScale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (enabled) backgroundColor else palette.surface,
        animationSpec = tween(160),
        label = "buttonBg"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (enabled) contentColor else palette.disabled
        ) {
            content()
        }
    }
}
