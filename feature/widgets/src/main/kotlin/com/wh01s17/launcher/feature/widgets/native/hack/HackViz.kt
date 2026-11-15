package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhColors
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.widgets.R
import com.wh01s17.launcher.feature.widgets.native.NativeWidgetSources
import kotlin.math.pow
import kotlin.math.roundToInt

/** Los visualizadores de TIDAL en la temática hack; tocar el espectro pasa al siguiente. */
internal enum class HackViz(
    val id: String,
) {
    /** Ecualizador de bloques: celdas cuadradas, rejilla apagada y picos que caen. */
    Blocks("blocks"),

    /** Como `cava` en ASCII: columnas de `#` en texto y el pico con `-`. */
    Cava("cava"),

    /** Osciloscopio: una línea en escalones que vibra alrededor del centro. */
    Scope("scope"),

    /** Como `xxd`: cada lectura es una fila de bytes en hex que sube, coloreada por intensidad. */
    Hexdump("hexdump"),
    ;

    fun next(): HackViz = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromId(id: String?): HackViz = entries.firstOrNull { it.id == id } ?: Blocks
    }
}

/**
 * El espectro de TIDAL con el visualizador [viz]. Sin el permiso de micrófono, `[viz]` lo pide
 * (plan.md R8); en pausa queda la rejilla vacía. Tocarlo cambia de visualizador ([onNext]).
 */
@Composable
internal fun HackSpectrum(
    sources: NativeWidgetSources,
    viz: HackViz,
    playing: Boolean,
    interactive: Boolean,
    onNext: () -> Unit,
    modifier: Modifier,
) {
    val permitted by sources.audioPermission.collectAsStateWithLifecycle()
    if (!permitted) {
        HackKeys { HackKey("viz", stringResource(R.string.native_tidal_visualizer), sources.requestAudio, interactive) }
        Spacer(modifier)
        return
    }
    val frames = rememberSpectrumFrames(sources, playing)
    val bands = frames.bands
    val peaks = frames.peaks
    val history = frames.history
    val description = stringResource(R.string.native_tidal_visualizer_shape, viz.id)
    val tap =
        if (interactive) {
            Modifier.semantics { contentDescription = description }.clickable(
                remember { MutableInteractionSource() },
                null,
                onClick = onNext,
            )
        } else {
            Modifier
        }
    val colors = WhTheme.colors
    BoxWithConstraints(modifier.then(tap)) {
        when (viz) {
            HackViz.Blocks -> Blocks(bands, peaks, colors, Modifier.fillMaxSize())
            HackViz.Scope -> Scope(bands, colors, Modifier.fillMaxSize())
            HackViz.Cava, HackViz.Hexdump -> {
                val style = WhTheme.typography.caption
                val measurer = rememberTextMeasurer()
                val glyph = remember(style, measurer) { measurer.measure("#", style).size }
                val columns = (constraints.maxWidth / glyph.width.coerceAtLeast(1)).coerceAtLeast(1)
                val rows = (constraints.maxHeight / glyph.height.coerceAtLeast(1)).coerceAtLeast(1)
                val text = if (viz == HackViz.Cava) cava(bands, peaks, columns, rows, colors) else hexdump(history, columns, rows, colors)
                WhText(text, style = style, maxLines = rows)
            }
        }
    }
}

/** Lo último que llegó del espectro, sus picos y las lecturas anteriores (para `hexdump`). */
private class SpectrumFrames(
    val bands: FloatArray,
    val peaks: FloatArray,
    val history: ArrayDeque<FloatArray>,
)

/**
 * Recoge el espectro mientras el inicio está a la vista (el `Visualizer` no escucha en segundo plano)
 * y procesa cada lectura una sola vez: contraste, picos que caen e historial.
 */
@Composable
private fun rememberSpectrumFrames(
    sources: NativeWidgetSources,
    playing: Boolean,
): SpectrumFrames {
    val peaks = remember { FloatArray(BANDS) }
    val history = remember { ArrayDeque<FloatArray>() }
    var bands by remember { mutableStateOf(FloatArray(BANDS)) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(sources, playing, lifecycle) {
        if (!playing) {
            bands = FloatArray(BANDS)
            return@LaunchedEffect
        }
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sources.spectrum.bands(BANDS).collect { raw ->
                // Más contraste: el espectro de la mezcla rara vez baja de la mitad y llenaba las filas de abajo.
                val next = FloatArray(BANDS) { raw[it].coerceIn(0f, 1f).pow(CONTRAST) }
                for (i in 0 until BANDS) peaks[i] = maxOf(next[i], peaks[i] - PEAK_FALL)
                history.addLast(next)
                while (history.size > MAX_HISTORY) history.removeFirst()
                bands = next
            }
        }
    }
    return SpectrumFrames(bands, peaks, history)
}

/** Bloques cuadrados por banda; lo apagado, un punto de la rejilla; el pico, una celda cian. */
@Composable
private fun Blocks(
    bands: FloatArray,
    peaks: FloatArray,
    colors: WhColors,
    modifier: Modifier,
) {
    val gap = WhTheme.spacing.borderSize
    Canvas(modifier) {
        val slot = size.width / BANDS
        val rows = (size.height / slot).toInt().coerceIn(1, MAX_ROWS)
        val cell = size.height / rows
        val inset = gap.toPx()
        val dot = inset * 2
        for (i in 0 until BANDS) {
            val level = (bands[i] * rows).roundToInt()
            val peak = (peaks[i] * rows).roundToInt().coerceAtMost(rows)
            for (r in 0 until rows) {
                val x = i * slot
                val y = size.height - (r + 1) * cell
                when {
                    r < level -> drawRect(colors.accent, Offset(x + inset, y + inset), Size(slot - inset * 2, cell - inset * 2))
                    r == peak - 1 -> drawRect(colors.cyan, Offset(x + inset, y + inset), Size(slot - inset * 2, cell - inset * 2))
                    else -> drawRect(colors.muted, Offset(x + slot / 2 - dot / 2, y + cell / 2 - dot / 2), Size(dot, dot))
                }
            }
        }
    }
}

/** Una línea en escalones que alterna arriba y abajo del centro, como la traza de un osciloscopio. */
@Composable
private fun Scope(
    bands: FloatArray,
    colors: WhColors,
    modifier: Modifier,
) {
    val stroke = WhTheme.spacing.borderSize * 2
    Canvas(modifier) {
        val mid = size.height / 2
        val step = size.width / BANDS
        val width = stroke.toPx()
        // El eje: una línea de puntos apagada.
        var x = 0f
        while (x < size.width) {
            drawRect(colors.muted, Offset(x, mid - width / 2), Size(width, width))
            x += width * AXIS_DOT_SPACING
        }
        var previous = mid
        for (i in 0 until BANDS) {
            val sign = if (i % 2 == 0) -1 else 1
            val y = mid + sign * bands[i] * mid * SCOPE_GAIN
            val left = i * step
            drawLine(colors.accent, Offset(left, previous), Offset(left, y), width)
            drawLine(colors.accent, Offset(left, y), Offset(left + step, y), width)
            previous = y
        }
    }
}

/** `cava` en texto: [columns] columnas de `#` de abajo arriba, el pico con `-` y lo vacío en blanco. */
private fun cava(
    bands: FloatArray,
    peaks: FloatArray,
    columns: Int,
    rows: Int,
    colors: WhColors,
): AnnotatedString {
    fun at(
        values: FloatArray,
        column: Int,
    ) = values[(column * BANDS / columns).coerceAtMost(BANDS - 1)]
    return buildAnnotatedString {
        for (row in rows - 1 downTo 0) {
            for (column in 0 until columns) {
                val level = (at(bands, column) * rows).roundToInt()
                val peak = (at(peaks, column) * rows).roundToInt()
                when {
                    row < level -> withStyle(SpanStyle(color = colors.accent)) { append('#') }
                    row == peak - 1 -> withStyle(SpanStyle(color = colors.cyan)) { append('-') }
                    else -> append(' ')
                }
            }
            if (row > 0) append('\n')
        }
    }
}

/**
 * `xxd`: una fila por lectura, la más nueva abajo; el desplazamiento apagado y cada byte (la banda de
 * 0 a 255) en hex, del apagado al acento y al cian según su intensidad.
 */
private fun hexdump(
    history: ArrayDeque<FloatArray>,
    columns: Int,
    rows: Int,
    colors: WhColors,
): AnnotatedString {
    // «0000: » ocupa 6 caracteres; cada byte, 3 («7f »).
    val bytes = ((columns - OFFSET_CHARS) / BYTE_CHARS).coerceIn(1, BANDS)
    val lines = history.toList().takeLast(rows)
    return buildAnnotatedString {
        lines.forEachIndexed { index, frame ->
            withStyle(SpanStyle(color = colors.darkForeground)) { append("%04x: ".format((history.size - lines.size + index) * bytes)) }
            for (b in 0 until bytes) {
                val value = frame[b * BANDS / bytes]
                val color =
                    when {
                        value > HOT -> colors.cyan
                        value > WARM -> colors.accent
                        else -> colors.muted
                    }
                withStyle(SpanStyle(color = color)) { append("%02x ".format((value * BYTE_MAX).roundToInt())) }
            }
            if (index < lines.lastIndex) append('\n')
        }
    }
}

private const val BANDS = 32
private const val CONTRAST = 1.8f
private const val PEAK_FALL = 0.02f
private const val MAX_ROWS = 24
private const val MAX_HISTORY = 64
private const val SCOPE_GAIN = 0.9f
private const val AXIS_DOT_SPACING = 4
private const val OFFSET_CHARS = 6
private const val BYTE_CHARS = 3
private const val BYTE_MAX = 255
private const val HOT = 0.6f
private const val WARM = 0.25f
