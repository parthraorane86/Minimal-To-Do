package com.personal.minimaltodo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.personal.minimaltodo.data.FontSizeOption
import com.personal.minimaltodo.data.HapticStrength
import com.personal.minimaltodo.data.ThemeMode
import com.personal.minimaltodo.data.UserSettingsEntity
import com.personal.minimaltodo.ui.components.MinimalInputField
import com.personal.minimaltodo.ui.components.MinimalToggleSwitch
import com.personal.minimaltodo.ui.components.SegmentedPillControl
import com.personal.minimaltodo.ui.haptics.LocalHapticEngine
import com.personal.minimaltodo.ui.theme.LocalMinimalPalette
import com.personal.minimaltodo.ui.theme.MotionSpecs
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: UserSettingsEntity,
    onUpdateName: (String) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateNotifications: (Boolean) -> Unit,
    onUpdateFontSize: (FontSizeOption) -> Unit,
    onUpdateHapticsEnabled: (Boolean) -> Unit,
    onUpdateHapticsStrength: (HapticStrength) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMinimalPalette.current
    val haptic = LocalHapticEngine.current
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    var nameInput by remember(settings.userName) { mutableStateOf(settings.userName) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onUpdateNotifications(granted)
    }

    val effectiveDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val backInteraction = remember { MutableInteractionSource() }
    val backPressed by backInteraction.collectIsPressedAsState()
    val backScale by animateFloatAsState(
        targetValue = if (backPressed) 0.92f else 1f,
        animationSpec = MotionSpecs.snappySpring(),
        label = "backBtnScale"
    )

    val sliderAlpha by animateFloatAsState(
        targetValue = if (settings.hapticsEnabled) 1f else 0.42f,
        animationSpec = tween(200),
        label = "hapticsSliderAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = backScale
                        scaleY = backScale
                    }
                    .clip(CircleShape)
                    .background(palette.surface)
                    .border(1.dp, palette.border, CircleShape)
                    .clickable(
                        interactionSource = backInteraction,
                        indication = null,
                        onClick = {
                            haptic.tick()
                            onBack()
                        }
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = "Profile & Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Name section
        SettingsSectionCard(title = "Name") {
            MinimalInputField(
                value = nameInput,
                onValueChange = {
                    nameInput = it
                    onUpdateName(it)
                },
                placeholder = "Your name"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Shown as a greeting on the Home screen",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Appearance section
        SettingsSectionCard(title = "Appearance") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dark Mode",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (effectiveDark) "Dark theme active" else "Light theme active",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }

                MinimalToggleSwitch(
                    checked = effectiveDark,
                    onCheckedChange = { turnDark ->
                        haptic.tick()
                        onUpdateThemeMode(if (turnDark) ThemeMode.DARK else ThemeMode.LIGHT)
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = palette.border, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Follow System",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Match your device's light or dark setting",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }

                MinimalToggleSwitch(
                    checked = settings.themeMode == ThemeMode.SYSTEM,
                    onCheckedChange = { followSystem ->
                        haptic.tick()
                        if (followSystem) {
                            onUpdateThemeMode(ThemeMode.SYSTEM)
                        } else {
                            onUpdateThemeMode(if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Haptics section (On/Off toggle + Low/Med/High slider shown but disabled when OFF)
        SettingsSectionCard(title = "Haptics") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Haptic Feedback",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Subtle vibrations on taps, checks, and gestures",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }

                MinimalToggleSwitch(
                    checked = settings.hapticsEnabled,
                    onCheckedChange = { enable ->
                        onUpdateHapticsEnabled(enable)
                        if (enable) {
                            haptic.preview(settings.hapticsStrength)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = palette.border, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(sliderAlpha)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Haptics Power",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = settings.hapticsStrength.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (settings.hapticsEnabled) palette.textPrimary else palette.disabled
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val currentIndex = settings.hapticsStrength.ordinal.toFloat()
                Slider(
                    value = currentIndex,
                    onValueChange = { raw ->
                        val next = HapticStrength.entries[raw.roundToInt().coerceIn(0, 2)]
                        if (next != settings.hapticsStrength) {
                            onUpdateHapticsStrength(next)
                            haptic.preview(next)
                        }
                    },
                    enabled = settings.hapticsEnabled,
                    valueRange = 0f..2f,
                    steps = 1,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.accent,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = palette.surface,
                        activeTickColor = palette.onAccent,
                        inactiveTickColor = palette.border,
                        disabledThumbColor = palette.disabled,
                        disabledActiveTrackColor = palette.disabled,
                        disabledInactiveTrackColor = palette.surface
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    HapticStrength.entries.forEach { level ->
                        Text(
                            text = level.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (level == settings.hapticsStrength && settings.hapticsEnabled) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                }
                            ),
                            color = if (level == settings.hapticsStrength && settings.hapticsEnabled) {
                                palette.textPrimary
                            } else {
                                palette.textSecondary
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Notifications section
        SettingsSectionCard(title = "Notifications") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Task Reminders",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Daily reminder when scheduled tasks are due",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }

                MinimalToggleSwitch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { enable ->
                        haptic.tick()
                        if (!enable) {
                            onUpdateNotifications(false)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onUpdateNotifications(true)
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            onUpdateNotifications(true)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Font Size section
        SettingsSectionCard(title = "Font Size") {
            SegmentedPillControl(
                items = FontSizeOption.entries,
                selectedItem = settings.fontSizeOption,
                onSelect = { option ->
                    if (option != settings.fontSizeOption) {
                        haptic.tick()
                        onUpdateFontSize(option)
                    }
                },
                labelProvider = { it.label },
                height = 42.dp,
                cornerRadius = 12.dp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val palette = LocalMinimalPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.elevatedCard)
            .border(1.dp, palette.border.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}
