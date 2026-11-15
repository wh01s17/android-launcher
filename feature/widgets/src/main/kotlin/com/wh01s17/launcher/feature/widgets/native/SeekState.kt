package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput

/**
 * La barra de tiempo de un reproductor, igual en todas las temáticas: tocar salta a ese punto y
 * arrastrar mueve la posición mostrada ([preview]) sin tocar la sesión hasta soltar, que es cuando se
 * busca (una sola orden a TIDAL, no una por fotograma). Si el gesto se cancela (el escritorio se lo
 * lleva), no se busca nada.
 */
@Stable
internal class SeekState {
    /** La posición bajo el dedo, de 0 a 1, mientras se arrastra; `null` si no. */
    var preview by mutableStateOf<Float?>(null)
        internal set
}

@Composable
internal fun rememberSeek() = remember { SeekState() }

/**
 * Los gestos de [state] sobre este nodo. [track] da, en píxeles del nodo y para su ancho, dónde empieza
 * y acaba la barra (en hack, sólo los `#…`, sin la etiqueta ni el tiempo). Sin [onSeek] no hace nada.
 */
internal fun Modifier.seekGestures(
    state: SeekState,
    onSeek: ((Float) -> Unit)?,
    track: (width: Int) -> ClosedFloatingPointRange<Float> = { 0f..it.toFloat() },
): Modifier {
    if (onSeek == null) return this
    return pointerInput(state, onSeek, track) {
        fun at(x: Float): Float {
            val range = track(size.width)
            val span = (range.endInclusive - range.start).coerceAtLeast(1f)
            return ((x - range.start) / span).coerceIn(0f, 1f)
        }
        awaitEachGesture {
            val down = awaitFirstDown()
            down.consume()
            var fraction = at(down.position.x)
            state.preview = fraction
            try {
                var done = false
                while (!done) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    done =
                        when {
                            change == null -> true
                            change.changedToUp() -> {
                                change.consume()
                                onSeek(fraction)
                                true
                            }
                            // Otro (el escritorio, el pager) se quedó el gesto: no se busca.
                            change.isConsumed -> true
                            else -> {
                                change.consume()
                                fraction = at(change.position.x)
                                state.preview = fraction
                                false
                            }
                        }
                }
            } finally {
                state.preview = null
            }
        }
    }
}
