package com.wh01s17.launcher.core.designsystem.theme

import com.wh01s17.launcher.core.designsystem.palette.PaletteRemap
import com.wh01s17.launcher.core.designsystem.palette.TidalampPalettes
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PaletteRemapTest {
    private val nord = PaletteRemap(TidalampPalettes.byName("nord"))

    @Test
    fun `phosphor leaves the whole theme untouched`() {
        val remap = PaletteRemap.Identity
        assertEquals(WhColors(), WhColors().remapped(remap::invoke))
        assertEquals(WhControls.create(), WhControls.create(remap::invoke))
        assertEquals(WhSurfaces.create(WhSpacing.create(1.25f)), WhSurfaces.create(WhSpacing.create(1.25f), remap::invoke))
        assertEquals(WhBorders(), WhBorders().remapped(remap::invoke))
    }

    @Test
    fun `another palette takes over accent, text and backgrounds`() {
        val palette = TidalampPalettes.byName("nord")
        val colors = WhColors().remapped(nord::invoke)
        assertEquals(palette.accent, colors.accent)
        assertEquals(palette.foreground, colors.foreground)
        assertEquals(palette.background, colors.background)
        assertEquals(palette.band, colors.lighterBackground)
        assertEquals(palette.dim, colors.darkForeground)
    }

    @Test
    fun `alpha is kept and warning colors do not change`() {
        val borders = WhBorders().remapped(nord::invoke)
        assertEquals(WhTokens.Border.activeStart.alpha, borders.activeStart.alpha, 0.01f)
        assertEquals(TidalampPalettes.byName("nord").accent.copy(alpha = borders.activeStart.alpha), borders.activeStart)
        val colors = WhColors().remapped(nord::invoke)
        assertEquals(WhTokens.Colors.red, colors.red)
        assertEquals(WhTokens.Colors.yellow, colors.yellow)
        assertEquals(WhDefaults.oledBackground, colors.oledBackground)
        assertNotEquals(WhTokens.Colors.cyan, colors.cyan)
    }

    @Test
    fun `controls and surfaces follow the palette`() {
        val palette = TidalampPalettes.byName("nord")
        assertEquals(palette.accent, WhControls.create(nord::invoke).selected.content)
        assertEquals(
            palette.foreground,
            WhSurfaces
                .create(WhSpacing.create(1.25f), nord::invoke)
                .bar.surface.text,
        )
    }
}
