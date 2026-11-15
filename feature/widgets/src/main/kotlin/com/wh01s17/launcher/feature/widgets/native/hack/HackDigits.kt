package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.min
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Cifras de bloques como las de `tty-clock`: cada carácter es una rejilla de 3×5 (los dos puntos, de
 * 1×5) de cuadrados planos, con un hueco fino entre ellos para que se lea el píxel. El tamaño de la
 * celda sale del ancho disponible (y de [maxHeight], si se da), así llenan el tile sin partirse.
 */
@Composable
internal fun HackDigits(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxHeight: Dp = Dp.Infinity,
    alignment: Alignment = Alignment.CenterStart,
) {
    val glyphs = text.map { GLYPHS[it] ?: BLANK }
    val columns = glyphs.sumOf { it.width } + (glyphs.size - 1).coerceAtLeast(0)
    val gap = WhTheme.spacing.borderSize
    BoxWithConstraints(modifier.fillMaxWidth().semantics { contentDescription = text }, contentAlignment = alignment) {
        // Con alto acotado (p. ej. `weight(1f)`), las cifras crecen hasta llenarlo sin pasarse del ancho.
        val available = if (constraints.hasBoundedHeight) this.maxHeight else Dp.Infinity
        val cell = min(min(maxWidth / columns, maxHeight / ROWS), available / ROWS)
        Canvas(Modifier.size(cell * columns, cell * ROWS)) {
            val side = cell.toPx()
            val inset = gap.toPx()
            var x = 0f
            for (glyph in glyphs) {
                for (row in 0 until ROWS) {
                    for (column in 0 until glyph.width) {
                        if (glyph.on(row, column)) {
                            drawRect(color, Offset(x + column * side, row * side), Size(side - inset, side - inset))
                        }
                    }
                }
                x += (glyph.width + 1) * side
            }
        }
    }
}

/** Un carácter de la fuente de bloques: [rows] de arriba abajo, `#` encendido. */
private class Glyph(
    vararg val rows: String,
) {
    val width: Int get() = rows.first().length

    fun on(
        row: Int,
        column: Int,
    ) = rows[row][column] == '#'
}

private const val ROWS = 5

private val BLANK = Glyph(".", ".", ".", ".", ".")

/** Las cifras de `tty-clock` (3×5) y los dos puntos. */
private val GLYPHS =
    mapOf(
        '0' to Glyph("###", "#.#", "#.#", "#.#", "###"),
        '1' to Glyph(".#.", "##.", ".#.", ".#.", "###"),
        '2' to Glyph("###", "..#", "###", "#..", "###"),
        '3' to Glyph("###", "..#", "###", "..#", "###"),
        '4' to Glyph("#.#", "#.#", "###", "..#", "..#"),
        '5' to Glyph("###", "#..", "###", "..#", "###"),
        '6' to Glyph("###", "#..", "###", "#.#", "###"),
        '7' to Glyph("###", "..#", "..#", "..#", "..#"),
        '8' to Glyph("###", "#.#", "###", "#.#", "###"),
        '9' to Glyph("###", "#.#", "###", "..#", "###"),
        ':' to Glyph(".", "#", ".", "#", "."),
        ' ' to BLANK,
    )
