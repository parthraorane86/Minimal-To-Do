package com.personal.minimaltodo.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.personal.minimaltodo.data.HapticStrength

/**
 * Strength-aware haptic engine using Android's VibrationEffect amplitude API (API 26+).
 * Scales crisp micro-pulses across Low / Medium / High while respecting the user's on/off toggle.
 */
@Immutable
class HapticEngine(
    private val context: Context,
    val enabled: Boolean,
    val strength: HapticStrength
) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun baseAmplitude(): Int = when (strength) {
        HapticStrength.LOW -> 60
        HapticStrength.MEDIUM -> 135
        HapticStrength.HIGH -> 225
    }

    private fun pulse(durationMs: Long, amplitudeScale: Float = 1f) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val amp = (baseAmplitude() * amplitudeScale).toInt().coerceIn(20, 255)
        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createOneShot(durationMs, amp)
        } else {
            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        v.vibrate(effect)
    }

    /** Light micro-tick for tabs, toggles, chips, calendar days, and buttons. */
    fun tick() = pulse(durationMs = 12L, amplitudeScale = 0.72f)

    /** Crisp confirmation pop when checking off a task or triggering edit. */
    fun confirm() = pulse(durationMs = 22L, amplitudeScale = 1.0f)

    /** Softer tap when unchecking a task. */
    fun uncheck() = pulse(durationMs = 10L, amplitudeScale = 0.55f)

    /** Tactile pickup thud when starting a Home drag-to-reorder gesture. */
    fun pickup() = pulse(durationMs = 26L, amplitudeScale = 1.05f)

    /** Tiny crisp notch when a dragged task swaps into a new slot. */
    fun slotSwap() = pulse(durationMs = 10L, amplitudeScale = 0.65f)

    /** Soft settle tap on drop. */
    fun drop() = pulse(durationMs = 14L, amplitudeScale = 0.75f)

    /**
     * Gentle ramping micro-tick during the 1.5s hold-to-edit gesture.
     * Escalates smoothly from subtle to crisp as progress approaches 1.0.
     */
    fun holdTick(progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)
        val duration = (6L + (5L * clamped)).toLong()
        val scale = 0.50f + 0.50f * clamped
        pulse(durationMs = duration, amplitudeScale = scale)
    }

    /** Distinct soft double-pulse on confirmed deletion. */
    fun delete() {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val amp = baseAmplitude().coerceIn(30, 255)
        val timings = longArrayOf(0L, 18L, 45L, 24L)
        val amplitudes = intArrayOf(0, (amp * 0.7f).toInt().coerceAtLeast(25), 0, amp)
        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            VibrationEffect.createWaveform(timings, -1)
        }
        v.vibrate(effect)
    }

    /** Preview pulse when the user adjusts the Low / Medium / High slider. */
    fun preview(previewStrength: HapticStrength) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val amp = when (previewStrength) {
            HapticStrength.LOW -> 60
            HapticStrength.MEDIUM -> 135
            HapticStrength.HIGH -> 225
        }
        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createOneShot(20L, amp)
        } else {
            VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        v.vibrate(effect)
    }
}

val LocalHapticEngine = staticCompositionLocalOf<HapticEngine> {
    error("LocalHapticEngine not provided")
}
