package com.wh01s17.launcher.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

// `Color` guarda el alfa en 8 bits: 0,70 se lee como 0,702. Un paso son 1/255 ≈ 0,0039.
private const val ALPHA_TOLERANCE = 0.005f

/** Los valores del tema salen de theme/wh01s17 (plan.md §4): si los dotfiles cambian, esto avisa. */
class WhThemeValuesTest {
    @Test
    fun `palette matches colors toml`() {
        val c = WhColors()
        assertEquals(Color(0xFF0A0E0F), c.background)
        assertEquals(Color(0xFF84C959), c.accent)
        assertEquals(Color(0xFF45D9EA), c.cyan)
        assertEquals(Color(0xFFCBD5CE), c.foreground)
        assertEquals(Color(0xFF243038), c.border)
        assertEquals(Color(0xFF33454F), c.muted)
        assertEquals(Color(0xFFFF6B66), c.red)
    }

    @Test
    fun `active border is a green to cyan gradient at 45 degrees, inactive is flat`() {
        val b = WhBorders()
        assertEquals(Color(0xEE84C959), b.activeStart)
        assertEquals(Color(0xEE45D9EA), b.activeEnd)
        assertEquals(45f, b.activeAngleDegrees, 0f)
        assertEquals(Color(0xAA243038), b.inactive)
    }

    @Test
    fun `typography scales desktop pixels by the font scale and never synthesizes weights`() {
        val t = WhTypography.create(1.25f)
        assertEquals(12.5f, t.caption.fontSize.value, 0.001f)
        assertEquals(15f, t.body.fontSize.value, 0.001f)
        assertEquals(20f, t.heading.fontSize.value, 0.001f)
        assertEquals(35f, t.displayLarge.fontSize.value, 0.001f)
        assertEquals(androidx.compose.ui.text.font.FontSynthesis.None, t.body.fontSynthesis)
        assertEquals(WhFonts.departureMono, t.body.fontFamily)
    }

    @Test
    fun `typography at scale 1 keeps the shell sizes`() {
        assertEquals(
            12f,
            WhTypography
                .create(1f)
                .body.fontSize.value,
            0.001f,
        )
    }

    @Test
    fun `spacing follows scale and scale-with-font`() {
        val s = WhSpacing.create(1.25f)
        assertEquals(37.5f, s.rowHeight.value, 0.001f)
        assertEquals(13.75f, s.controlPaddingX.value, 0.001f)
        assertEquals(10f, s.controlGap.value, 0.001f)
        assertEquals(1f, WhSpacing.create(1f).borderSize.value, 0f)
        assertEquals(48f, s.touchTarget.value, 0f)
    }

    @Test
    fun `control states have the alphas of shell controls toml`() {
        val controls = WhControls.create()
        assertEquals(0.035f, controls.normal.fill.alpha, ALPHA_TOLERANCE)
        assertEquals(Color(0xFF45D9EA).copy(alpha = 0.8f), controls.focus.border)
        assertEquals(0.16f, controls.selected.fill.alpha, ALPHA_TOLERANCE)
        assertEquals(0.22f, controls.pressed.fill.alpha, ALPHA_TOLERANCE)
        assertEquals(0.35f, controls.textSelection.alpha, ALPHA_TOLERANCE)
    }

    @Test
    fun `surfaces apply their alpha`() {
        val surfaces = WhSurfaces.create(WhSpacing.create(1.25f))
        assertEquals(0.98f, surfaces.menu.surface.background.alpha, ALPHA_TOLERANCE)
        assertEquals(0.65f, surfaces.menu.scrim.alpha, ALPHA_TOLERANCE)
        assertEquals(0.70f, surfaces.launcher.scrim.alpha, ALPHA_TOLERANCE)
        assertEquals(0.10f, surfaces.menu.selectedBackground.alpha, ALPHA_TOLERANCE)
        assertEquals(Color(0xFFFF6B66), surfaces.bar.active)
        assertEquals(0.97f, surfaces.tooltip.background.alpha, ALPHA_TOLERANCE)
    }
}
