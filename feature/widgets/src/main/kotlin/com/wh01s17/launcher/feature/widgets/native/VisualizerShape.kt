package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.wh01s17.launcher.core.designsystem.palette.PlayerPalette
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import kotlin.math.max

/**
 * Las formas del analizador de tidalamp (`analyzer.py`, ajuste `visualizer`). `fine` va primero: es
 * la de fábrica del widget; tocar el analizador pasa a la siguiente.
 */
enum class VisualizerShape(
    val configName: String,
) {
    /** La traza del espectro sobre una rejilla de puntos, unida en un trazo (el Braille de tidalamp). */
    Fine("fine"),

    /** Barras con picos que caen despacio. */
    Bars("bars"),

    /** Barras hacia arriba y hacia abajo desde una línea central. */
    Mirror("mirror"),

    /** El contorno del espectro como una línea. */
    Curve("curve"),
    ;

    fun next(): VisualizerShape = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromConfig(name: String?): VisualizerShape = entries.firstOrNull { it.configName == name } ?: Fine
    }
}

/**
 * Dibuja [bands] (0..1) con la forma [shape]. Lo fuerte (por encima de [LOUD]) va en el color del
 * texto y el resto en acento, como el degradado de tidalamp; [peaks] son los picos de `bars`.
 */
@Composable
internal fun Spectrum(
    bands: FloatArray,
    peaks: FloatArray,
    shape: VisualizerShape,
    palette: PlayerPalette,
    modifier: Modifier,
) {
    val unit = WhTheme.spacing.borderSize
    Box(
        modifier.drawBehind {
            if (bands.isEmpty()) return@drawBehind
            val px = unit.toPx()
            when (shape) {
                VisualizerShape.Fine -> fine(bands, palette, px)
                VisualizerShape.Bars -> bars(bands, peaks, palette)
                VisualizerShape.Mirror -> mirror(bands, palette, px)
                VisualizerShape.Curve -> curve(bands, palette, px)
            }
        },
    )
}

private fun color(
    level: Float,
    palette: PlayerPalette,
) = if (level > LOUD) palette.foreground else palette.accent

/**
 * Dos columnas de puntos por banda; cada columna enciende los puntos entre su altura y la mitad del
 * camino hasta sus vecinas, así la forma es un trazo y no puntos sueltos (como `_render_fine`).
 */
private fun DrawScope.fine(
    bands: FloatArray,
    palette: PlayerPalette,
    unit: Float,
) {
    val radius = unit * DOT_RADIUS
    val pitch = radius * DOT_PITCH
    val rows = max(1, (size.height / pitch).toInt())
    val columns = bands.size * 2
    val step = size.width / columns
    val heights = IntArray(columns) { x -> (bands[x / 2] * (rows - 1)).toInt() }
    for (x in 0 until columns) {
        val joins = listOfNotNull(heights.getOrNull(x - 1), heights.getOrNull(x + 1)).map { (heights[x] + it) / 2 }
        val low = (joins + heights[x]).min()
        val high = (joins + heights[x]).max()
        val tint = color(bands[x / 2], palette)
        for (dot in low..high) drawCircle(tint, radius, Offset(step * (x + HALF), size.height - pitch * (dot + HALF)))
    }
}

private fun DrawScope.bars(
    bands: FloatArray,
    peaks: FloatArray,
    palette: PlayerPalette,
) {
    val step = size.width / bands.size
    val width = step * BAR_WIDTH
    bands.forEachIndexed { i, level ->
        val left = step * i + (step - width) / 2
        val top = size.height * (1 - level)
        drawRect(color(level, palette), Offset(left, top), size.copy(width = width, height = size.height - top))
        val peak = peaks.getOrNull(i) ?: return@forEachIndexed
        if (peak > PEAK_MIN) {
            val y = size.height * (1 - peak)
            drawLine(palette.foreground, Offset(left, y), Offset(left + width, y), width / BAR_WIDTH * PEAK_THICKNESS)
        }
    }
}

private fun DrawScope.mirror(
    bands: FloatArray,
    palette: PlayerPalette,
    unit: Float,
) {
    val step = size.width / bands.size
    val width = step * BAR_WIDTH
    val middle = size.height / 2
    drawLine(palette.muted, Offset(0f, middle), Offset(size.width, middle), unit)
    bands.forEachIndexed { i, level ->
        val left = step * i + (step - width) / 2
        val half = middle * level
        drawRect(color(level, palette), Offset(left, middle - half), size.copy(width = width, height = half * 2))
    }
}

private fun DrawScope.curve(
    bands: FloatArray,
    palette: PlayerPalette,
    unit: Float,
) {
    val step = size.width / bands.size
    val path = Path()
    bands.forEachIndexed { i, level ->
        val point = Offset(step * (i + HALF), size.height * (1 - level))
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    drawPath(path, palette.accent, style = Stroke(width = unit * CURVE_WIDTH))
}

/** Los picos de `bars`: suben con la banda y caen [PEAK_FALL] por captura, como en tidalamp. */
internal fun fallPeaks(
    peaks: FloatArray,
    bands: FloatArray,
) {
    for (i in peaks.indices) peaks[i] = max(bands.getOrElse(i) { 0f }, peaks[i] - PEAK_FALL)
}

private const val LOUD = 0.8f
private const val HALF = 0.5f
private const val DOT_RADIUS = 1.2f
private const val DOT_PITCH = 2.6f
private const val BAR_WIDTH = 0.7f
private const val PEAK_MIN = 0.02f
private const val PEAK_THICKNESS = 0.15f
private const val PEAK_FALL = 0.015f
private const val CURVE_WIDTH = 2f
