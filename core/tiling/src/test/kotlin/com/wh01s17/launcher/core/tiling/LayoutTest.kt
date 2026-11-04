package com.wh01s17.launcher.core.tiling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Distribuciones de referencia, calculadas a mano. */
class LayoutTest {
    private val a = TileId(1)
    private val b = TileId(2)
    private val c = TileId(3)

    // Un teléfono en vertical: 1000 × 2000, 4 px entre tiles y 2 al borde.
    private val portrait = Rect(0, 0, 1000, 2000)
    private val gaps = Gaps(inner = 4, outer = 2)

    private fun three() = Workspace().insert(a).insert(b).insert(c)

    @Test
    fun `an empty workspace places nothing`() {
        assertEquals(LayoutResult.Empty, layout(Workspace(), portrait, gaps))
    }

    @Test
    fun `one tile takes the whole area minus the outer gap`() {
        assertEquals(mapOf(a to Rect(2, 2, 998, 1998)), layout(Workspace().insert(a), portrait, gaps).tiled)
    }

    @Test
    fun `dwindle splits the long side, then the long side of the new half`() {
        val tiles = layout(three(), portrait, gaps).tiled
        // 1996 de alto − 4 de hueco = 1992 → 996 + 996. La mitad de abajo es cuadrada: se parte a lo ancho.
        assertEquals(Rect(2, 2, 998, 998), tiles[a])
        assertEquals(Rect(2, 1002, 498, 1998), tiles[b])
        assertEquals(Rect(502, 1002, 998, 1998), tiles[c])
    }

    @Test
    fun `master puts the master on top in portrait and on the left in landscape`() {
        val master = three().withLayout(TilingLayout.Master)
        val tiles = layout(master, portrait, gaps).tiled
        // 1992 × 0,55 = 1095,6 → 1096.
        assertEquals(Rect(2, 2, 998, 1098), tiles[a])
        assertEquals(Rect(2, 1102, 498, 1998), tiles[b])
        assertEquals(Rect(502, 1102, 998, 1998), tiles[c])
        val wide = layout(master, Rect(0, 0, 2000, 1000), gaps).tiled
        assertEquals(Rect(2, 2, 1098, 998), wide[a])
        assertTrue(wide.getValue(b).left == 1102 && wide.getValue(c).left == 1102)
    }

    @Test
    fun `the stack hands the leftover pixels to its last tile`() {
        val rects = stackRects(Rect(0, 0, 10, 100), Orientation.Vertical, 3, 2)
        assertEquals(listOf(Rect(0, 0, 10, 32), Rect(0, 34, 10, 66), Rect(0, 68, 10, 100)), rects)
    }

    @Test
    fun `a gap wider than the side shrinks instead of going negative`() {
        val (first, second) = splitRect(Rect(0, 0, 3, 10), Orientation.Horizontal, 0.5f, 10)
        assertEquals(0, first.width)
        assertEquals(0, second.width)
        assertEquals(3, second.left)
    }

    @Test
    fun `fullscreen shows only that tile over the whole area`() {
        val ws = three().focus(b).toggleFullscreen()
        assertEquals(LayoutResult(mapOf(b to Rect(2, 2, 998, 1998)), emptyMap(), emptyMap(), b), layout(ws, portrait, gaps))
        assertEquals(null, ws.toggleFullscreen().fullscreen)
    }

    @Test
    fun `floating tiles sit on top at their fraction of the area`() {
        val ws = three().toggleFloating(c)
        val result = layout(ws, portrait, gaps)
        assertEquals(setOf(a, b), result.tiled.keys)
        assertEquals(Rect(2 + 199, 2 + 399, 2 + 199 + 597, 2 + 399 + 1197), result.floating[c])
        assertEquals(listOf(a, b, c), result.visible.keys.toList())
    }

    @Test
    fun `floating bounds never leave the area`() {
        val off = FloatingBounds(x = 0.9f, y = -0.5f, width = 0.5f, height = 0.5f)
        val rect = off.toRect(Rect(0, 0, 100, 100))
        assertEquals(Rect(50, 0, 100, 50), rect)
    }

    @Test
    fun `layout names follow the config`() {
        assertEquals(TilingLayout.Master, TilingLayout.fromConfig("master"))
        assertEquals(null, TilingLayout.fromConfig("spiral"))
    }
}
