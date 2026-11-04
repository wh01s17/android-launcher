package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.binding.Gesture
import kotlin.math.abs

/**
 * Deslizar con dos dedos (plan.md §3): emite `two-finger-swipe-*` según hacia dónde se movió el
 * centro de los dedos. Mira los eventos antes que el pager y los tiles (pase `Initial`) y, desde que
 * hay dos dedos, los consume: el pager no pasa de página ni un tile se abre a la vez. Un arrastre
 * emite un solo gesto, al recorrer una fila del menú.
 */
@Composable
internal fun Modifier.twoFingerSwipes(
    enabled: Boolean,
    onGesture: (Gesture) -> Unit,
): Modifier {
    if (!enabled) return this
    val threshold = with(LocalDensity.current) { WhTheme.spacing.menuRowHeight.toPx() }
    val latest by rememberUpdatedState(onGesture)
    return pointerInput(threshold) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var start: Offset? = null
            var fired = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val center = pressed.fold(Offset.Zero) { sum, change -> sum + change.position } / pressed.size.toFloat()
                    val from = start ?: center.also { start = it }
                    val delta = center - from
                    if (!fired && delta.getDistance() > threshold) {
                        fired = true
                        latest(directionOf(delta))
                    }
                }
                // Desde el segundo dedo y hasta soltar todos, el gesto es nuestro.
                if (start != null) event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

private fun directionOf(delta: Offset): Gesture =
    if (abs(delta.x) >= abs(delta.y)) {
        if (delta.x < 0) Gesture.TwoFingerSwipeLeft else Gesture.TwoFingerSwipeRight
    } else {
        if (delta.y < 0) Gesture.TwoFingerSwipeUp else Gesture.TwoFingerSwipeDown
    }
