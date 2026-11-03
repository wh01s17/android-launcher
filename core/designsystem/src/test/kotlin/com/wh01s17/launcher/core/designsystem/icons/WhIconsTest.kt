package com.wh01s17.launcher.core.designsystem.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Font
import java.io.File

class WhIconsTest {
    @Test
    fun `every glyph exists in the bundled pixel icons font`() {
        System.setProperty("java.awt.headless", "true")
        val font = Font.createFont(Font.TRUETYPE_FONT, File("src/main/res/font/wh01s17_pixel_icons.ttf"))
        val missing =
            WhIcons.catalog.filterValues { glyph ->
                !font.canDisplay(glyph.text.codePointAt(0))
            }
        assertTrue("faltan glifos en la fuente: ${missing.keys}", missing.isEmpty())
    }

    @Test
    fun `names are unique and each glyph is a single code point`() {
        val glyphs = WhIcons.catalog.values.map { it.text }
        assertEquals("hay dos iconos con el mismo glifo", glyphs.size, glyphs.toSet().size)
        assertTrue(WhIcons.catalog.values.all { it.text.codePointCount(0, it.text.length) == 1 })
    }

    @Test
    fun `code points above the BMP survive as one glyph`() {
        assertEquals(2, WhIcons.Apps.text.length)
        assertEquals(0xF003B, WhIcons.Apps.text.codePointAt(0))
    }
}
