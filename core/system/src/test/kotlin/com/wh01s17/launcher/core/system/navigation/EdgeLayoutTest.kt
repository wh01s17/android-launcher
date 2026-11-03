package com.wh01s17.launcher.core.system.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EdgeLayoutTest {
    // El Redmi en vertical: 1220 × 2712, franjas de 42/48 px (14/16 dp a 3×), barra de estado de 110 px.
    private val redmi = EdgeLayoutInput(1220, 2712, sideThicknessPx = 42, bottomThicknessPx = 48, topExclusionPx = 110)

    @Test
    fun `strips hug the edges below the status bar`() {
        val strips = layoutStrips(redmi)
        assertEquals(StripRect(0, 110, 42, 2712 - 48 - 110), strips[Edge.Left])
        assertEquals(StripRect(1220 - 42, 110, 42, 2712 - 48 - 110), strips[Edge.Right])
        assertEquals(StripRect(0, 2712 - 48, 1220, 48), strips[Edge.Bottom])
    }

    @Test
    fun `the keyboard hides the bottom strip and shortens the sides`() {
        val strips = layoutStrips(redmi.copy(imeTop = 1700))
        assertNull(strips[Edge.Bottom])
        assertEquals(1700 - 110, strips[Edge.Left]!!.height)
    }

    @Test
    fun `a huge keyboard leaves no side strips`() {
        val strips = layoutStrips(redmi.copy(imeTop = 130))
        assertNull(strips[Edge.Left])
        assertNull(strips[Edge.Right])
    }

    @Test
    fun `suspended or disabled edges receive no touches`() {
        assertEquals(setOf(null), layoutStrips(redmi.copy(suspended = true)).values.toSet())
        val onlyBack = layoutStrips(redmi.copy(enabled = setOf(Edge.Left, Edge.Right)))
        assertNull(onlyBack[Edge.Bottom])
        assertEquals(42, onlyBack[Edge.Left]!!.width)
    }
}
