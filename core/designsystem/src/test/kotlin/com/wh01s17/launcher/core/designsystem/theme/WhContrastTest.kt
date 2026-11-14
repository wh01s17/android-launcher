package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.wh01s17.launcher.core.designsystem.palette.PaletteRemap
import com.wh01s17.launcher.core.designsystem.palette.TidalampPalettes
import com.wh01s17.launcher.core.designsystem.palette.forOled
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contraste AA del texto con los tokens y con cada paleta (plan.md §7 Hito 11). */
class WhContrastTest {
    @Test
    fun `ratio matches WCAG reference values`() {
        assertEquals(21f, WhContrast.ratio(Color.White, Color.Black), 0.01f)
        assertEquals(1f, WhContrast.ratio(Color.Gray, Color.Gray), 0.01f)
        // #cbd5ce sobre #0a0e0f, calculado a mano con la fórmula de WCAG 2.2.
        assertEquals(12.88f, WhContrast.ratio(WhTokens.Colors.foreground, WhTokens.Colors.background), 0.02f)
    }

    @Test
    fun `ensure leaves readable colors alone and lifts the rest just enough`() {
        val tile = WhTokens.Colors.lighterBackground
        assertEquals(WhTokens.Colors.foreground, WhContrast.ensure(WhTokens.Colors.foreground, listOf(tile)))
        val dim = WhTokens.Colors.darkForeground
        assertTrue(WhContrast.ratio(dim, tile) < WhContrast.AA_TEXT)
        val lifted = WhContrast.ensure(dim, listOf(tile))
        assertTrue(WhContrast.ratio(lifted, tile) >= WhContrast.AA_TEXT)
        // Lo justo: un paso menos ya no llegaría (1/64 de la mezcla hacia el blanco).
        assertTrue(WhContrast.ratio(lifted, tile) < WhContrast.AA_TEXT + 0.3f)
    }

    @Test
    fun `phosphor only moves the dim text`() {
        val tokens = WhColors()
        val readable = tokens.readable(surface = WhTokens.Bar.background)
        assertNotEquals(tokens.darkForeground, readable.darkForeground)
        assertEquals(tokens.copy(darkForeground = readable.darkForeground), readable)
    }

    @Test
    fun `every palette reaches AA for text on every text surface`() {
        val failures =
            TidalampPalettes.all.flatMap { listOf(it, it.forOled()) }.flatMap { palette ->
                val remap = PaletteRemap(palette)
                val surface = remap(WhTokens.Bar.background)
                val raw = WhColors().remapped(remap::invoke)
                val colors = raw.readable(surface)
                // La fila seleccionada se rellena con el acento de la paleta (WhSurfaces), no con el ajustado.
                val selectedRow = raw.accent.copy(alpha = 0.10f).compositeOver(surface)
                val backgrounds = listOf(colors.background, surface, colors.lighterBackground, selectedRow)
                val texts =
                    listOf(colors.foreground, colors.darkForeground, colors.lightForeground, colors.accent, colors.cyan) +
                        listOf(colors.red, colors.orange, colors.yellow, colors.blue, colors.magenta)
                texts.flatMap { text ->
                    backgrounds.filter { WhContrast.ratio(text, it) < WhContrast.AA_TEXT }.map { "${palette.name}: $text on $it" }
                }
            }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `widget palettes reach AA on their background and band`() {
        val failures =
            TidalampPalettes.all.flatMap { listOf(it, it.forOled()) }.map { it.readable() }.flatMap { p ->
                listOf(p.foreground, p.dim, p.accent).flatMap { text ->
                    listOf(p.background, p.band).filter { WhContrast.ratio(text, it) < WhContrast.AA_TEXT }.map { "${p.name}: $text" }
                }
            }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
