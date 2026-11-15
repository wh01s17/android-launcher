package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import kotlinx.coroutines.delay

/**
 * Líneas que se deslizan cuando no caben, como el `Glide` de tidalamp: la que cabe va entera; la que
 * no, espera un momento, avanza un carácter cada [STEP_MILLIS] hasta que se ve su final, espera otra
 * vez y vuelve. Recortar escondía el final del título para siempre, y un bucle que da la vuelta lo lee
 * en dos trozos; ir y volver lo enseña entero y en orden.
 *
 * Todas las líneas de un [HackGlide] comparten la fase: cada una se para en su final, así que una corta
 * espera a que acabe la larga y salen juntas otra vez. Sólo se mueve con fotogramas (el inicio a la
 * vista): fuera de pantalla el bucle se queda esperando el siguiente.
 */
@Stable
internal class HackGlide {
    /** Cuántos caracteres le sobran a cada línea, por su clave. */
    internal val overflows = mutableStateMapOf<String, Int>()

    /** Caracteres desplazados ahora; cada línea lo limita a lo que le sobra. */
    internal var step by mutableIntStateOf(0)
}

/** Un [HackGlide] que vuelve a empezar desde el principio cuando cambia alguna de [texts]. */
@Composable
internal fun rememberHackGlide(vararg texts: String?): HackGlide {
    val glide = remember { HackGlide() }
    val longest = glide.overflows.values.maxOrNull() ?: 0
    LaunchedEffect(glide, longest, *texts) {
        glide.step = 0
        if (longest <= 0) return@LaunchedEffect
        while (true) {
            pause(HOLD_MILLIS)
            while (glide.step < longest) {
                pause(STEP_MILLIS)
                glide.step++
            }
            pause(HOLD_MILLIS)
            while (glide.step > 0) {
                pause(STEP_MILLIS)
                glide.step--
            }
        }
    }
    return glide
}

/** Espera [millis] y después al siguiente fotograma: sin nada a la vista no se avanza. */
private suspend fun pause(millis: Long) {
    delay(millis)
    withFrameNanos { }
}

/** `> clave valor` como [HackLine], pero el valor se desliza con [glide] si no cabe. */
@Composable
internal fun HackGlideLine(
    glide: HackGlide,
    key: String,
    value: String,
    color: Color = WhTheme.colors.foreground,
) {
    val style = WhTheme.typography.bodySmall
    val measurer = rememberTextMeasurer()
    val cell =
        remember(measurer, style) {
            measurer
                .measure("0", style)
                .size.width
                .coerceAtLeast(1)
        }
    DisposableEffect(glide, key) { onDispose { glide.overflows.remove(key) } }
    Row {
        WhText("> ${key.padEnd(HACK_KEY_WIDTH)} ", style = style, color = WhTheme.colors.darkForeground, maxLines = 1)
        WhText(
            value,
            Modifier
                .weight(1f)
                .clipToBounds()
                .layout { measurable, constraints ->
                    // El texto se mide entero; lo que no cabe se queda fuera del recorte y se desplaza.
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
                    val overflow = (placeable.width - constraints.maxWidth).coerceAtLeast(0)
                    val chars = (overflow + cell - 1) / cell
                    if (glide.overflows[key] != chars) glide.overflows[key] = chars
                    layout(constraints.constrainWidth(placeable.width), constraints.constrainHeight(placeable.height)) {
                        placeable.placeRelative(-minOf(glide.step * cell, overflow), 0)
                    }
                },
            style = style,
            color = color,
            maxLines = 1,
        )
    }
}

/** Unos dos segundos quieto en cada extremo y un carácter cada 0,3 s, como tidalamp. */
private const val HOLD_MILLIS = 2_100L
private const val STEP_MILLIS = 300L
