package com.personal.minimaltodo.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.personal.minimaltodo.data.FontSizeOption
import com.personal.minimaltodo.data.ThemeMode

@Immutable
data class MinimalPalette(
    val background: Color,
    val surface: Color,
    val elevatedCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val border: Color,
    val disabled: Color,
    val onAccent: Color,
    val isDark: Boolean
)

val LightMinimalPalette = MinimalPalette(
    background = Color(0xFFF5F5F0),
    surface = Color(0xFFE5E5DF),
    elevatedCard = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF333333),
    textSecondary = Color(0xFF77776F),
    accent = Color(0xFF6C8A72),
    border = Color(0xFFD5D5CD),
    disabled = Color(0xFFA0A097),
    onAccent = Color(0xFFF5F5F0),
    isDark = false
)

val DarkMinimalPalette = MinimalPalette(
    background = Color(0xFF171816),
    surface = Color(0xFF222420),
    elevatedCard = Color(0xFF2D302B),
    textPrimary = Color(0xFFEAECE6),
    textSecondary = Color(0xFFA6A69B),
    accent = Color(0xFF8BA18D),
    border = Color(0xFF383A35),
    disabled = Color(0xFF66685F),
    onAccent = Color(0xFF171816),
    isDark = true
)

val LocalMinimalPalette = staticCompositionLocalOf { LightMinimalPalette }

object MotionSpecs {
    fun <T> snappySpring() = spring<T>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> bouncySpring() = spring<T>(
        dampingRatio = 0.65f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> gentleSpring() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessLow
    )
}

private fun scaledTypography(scale: Float): Typography {
    return Typography(
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = (26f * scale).sp,
            lineHeight = (32f * scale).sp,
            letterSpacing = (-0.4f).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20f * scale).sp,
            lineHeight = (26f * scale).sp,
            letterSpacing = (-0.2f).sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = (16f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = (16f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = (14f * scale).sp,
            lineHeight = (18f * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * scale).sp,
            lineHeight = (16f * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = (11f * scale).sp,
            lineHeight = (14f * scale).sp
        )
    )
}

@Composable
fun MinimalTodoTheme(
    themeMode: ThemeMode,
    fontSizeOption: FontSizeOption,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val target = if (useDark) DarkMinimalPalette else LightMinimalPalette
    val colorAnimSpec = tween<Color>(durationMillis = 240)

    val animatedPalette = MinimalPalette(
        background = animateColorAsState(target.background, colorAnimSpec, label = "bg").value,
        surface = animateColorAsState(target.surface, colorAnimSpec, label = "surface").value,
        elevatedCard = animateColorAsState(target.elevatedCard, colorAnimSpec, label = "card").value,
        textPrimary = animateColorAsState(target.textPrimary, colorAnimSpec, label = "textPrimary").value,
        textSecondary = animateColorAsState(target.textSecondary, colorAnimSpec, label = "textSecondary").value,
        accent = animateColorAsState(target.accent, colorAnimSpec, label = "accent").value,
        border = animateColorAsState(target.border, colorAnimSpec, label = "border").value,
        disabled = animateColorAsState(target.disabled, colorAnimSpec, label = "disabled").value,
        onAccent = animateColorAsState(target.onAccent, colorAnimSpec, label = "onAccent").value,
        isDark = useDark
    )

    val materialScheme = if (useDark) {
        darkColorScheme(
            primary = animatedPalette.accent,
            onPrimary = animatedPalette.onAccent,
            background = animatedPalette.background,
            onBackground = animatedPalette.textPrimary,
            surface = animatedPalette.surface,
            onSurface = animatedPalette.textPrimary,
            surfaceVariant = animatedPalette.elevatedCard,
            onSurfaceVariant = animatedPalette.textSecondary,
            outline = animatedPalette.border
        )
    } else {
        lightColorScheme(
            primary = animatedPalette.accent,
            onPrimary = animatedPalette.onAccent,
            background = animatedPalette.background,
            onBackground = animatedPalette.textPrimary,
            surface = animatedPalette.surface,
            onSurface = animatedPalette.textPrimary,
            surfaceVariant = animatedPalette.elevatedCard,
            onSurfaceVariant = animatedPalette.textSecondary,
            outline = animatedPalette.border
        )
    }

    CompositionLocalProvider(LocalMinimalPalette provides animatedPalette) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = scaledTypography(fontSizeOption.scale),
            content = content
        )
    }
}
