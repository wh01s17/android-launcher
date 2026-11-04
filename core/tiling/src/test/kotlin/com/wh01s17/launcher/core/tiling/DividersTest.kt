package com.wh01s17.launcher.core.tiling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Bordes arrastrables del modo edición y `resizeactive` de Hyprland. */
class DividersTest {
    private val a = TileId(1)
    private val b = TileId(2)
    private val c = TileId(3)
    private val portrait = Rect(0, 0, 1000, 2000)
    private val gaps = Gaps(inner = 4, outer = 2)

    private fun three() = Workspace().insert(a).insert(b).insert(c)

    @Test
    fun `dwindle has one divider per split, with its gap strip`() {
        val list = dividers(three(), portrait, gaps)
        assertEquals(
            listOf(
                Divider(emptyList(), Orientation.Vertical, Rect(2, 2, 998, 1998), Rect(2, 998, 998, 1002)),
                Divider(listOf(true), Orientation.Horizontal, Rect(2, 1002, 998, 1998), Rect(498, 1002, 502, 1998)),
            ),
            list,
        )
        assertEquals(1000, list[0].center)
        assertEquals(500, list[1].center)
    }

    @Test
    fun `master has a single divider and none without two slots or in fullscreen`() {
        val master = three().withLayout(TilingLayout.Master)
        assertEquals(
            listOf(Divider(null, Orientation.Vertical, Rect(2, 2, 998, 1998), Rect(2, 1098, 998, 1102))),
            dividers(master, portrait, gaps),
        )
        assertEquals(emptyList<Divider>(), dividers(Workspace().insert(a), portrait, gaps))
        assertEquals(emptyList<Divider>(), dividers(Workspace(), portrait, gaps))
        assertEquals(emptyList<Divider>(), dividers(three().toggleFullscreen(), portrait, gaps))
    }

    @Test
    fun `dragging a divider moves the border to the finger`() {
        val ws = three()
        val inner = dividers(ws, portrait, gaps)[1]
        // El centro del hueco a 302 px: el primer lado mide 300 de 992 disponibles.
        val moved = ws.moveDivider(inner, 302, gaps)
        val rects = layout(moved, portrait, gaps).tiled
        assertEquals(302, rects.getValue(b).right + 2)
        assertEquals(Rect(2, 2, 998, 998), rects[a])
        // Dejarlo donde está no cambia la proporción.
        assertEquals(ws, ws.moveDivider(inner, inner.center, gaps))
    }

    @Test
    fun `divider ratios stay within limits and stale paths are ignored`() {
        val ws = three()
        val outer = dividers(ws, portrait, gaps)[0]
        assertEquals(SplitRatio.MIN, (ws.moveDivider(outer, -500, gaps).root as Split).ratio)
        assertEquals(SplitRatio.MAX, (ws.moveDivider(outer, 5000, gaps).root as Split).ratio)
        val stale = outer.copy(path = listOf(false, true))
        assertEquals(ws, ws.moveDivider(stale, 700, gaps))
        assertEquals(Workspace(), Workspace().moveDivider(outer, 700, gaps))
        val flat = outer.copy(bounds = Rect(0, 0, 4, 4))
        assertEquals(ws, ws.moveDivider(flat, 2, gaps))
    }

    @Test
    fun `dragging the master divider changes the master ratio`() {
        val master = three().withLayout(TilingLayout.Master)
        val divider = dividers(master, portrait, gaps).single()
        // En vertical se arrastra en y: 2 de margen + 2 de medio hueco + la mitad de 1992.
        assertEquals(0.5f, master.moveDivider(divider, 2 + 2 + 996, gaps).masterRatio, 1e-3f)
    }

    @Test
    fun `resizeBy moves the free edge like resizeactive`() {
        // A arriba, B abajo a la izquierda, C abajo a la derecha.
        val fromC = three()
        assertFalse(fromC.hasEdge(Direction.Right, portrait, gaps))
        assertTrue(fromC.hasEdge(Direction.Left, portrait, gaps))
        // C está pegado a la derecha: dx < 0 mueve su borde izquierdo y C crece.
        val grown = layout(fromC.resizeBy(-100, 0, portrait, gaps), portrait, gaps).tiled.getValue(c)
        assertEquals(402, grown.left)
        // B tiene borde a la derecha: dx < 0 lo encoge.
        val shrunk = layout(fromC.focus(b).resizeBy(-100, 0, portrait, gaps), portrait, gaps).tiled.getValue(b)
        assertEquals(398, shrunk.right)
        // En vertical: B tiene borde arriba (con A), no abajo.
        val taller = layout(fromC.focus(b).resizeBy(0, -100, portrait, gaps), portrait, gaps).tiled.getValue(b)
        assertEquals(902, taller.top)
        assertEquals(fromC, fromC.resizeBy(0, 0, portrait, gaps))
    }

    @Test
    fun `hasEdge in master and for tiles without borders`() {
        val master = three().withLayout(TilingLayout.Master).focus(a)
        assertTrue(master.hasEdge(Direction.Down, portrait, gaps))
        assertFalse(master.hasEdge(Direction.Up, portrait, gaps))
        assertFalse(master.hasEdge(Direction.Right, portrait, gaps))
        assertFalse(three().toggleFloating().hasEdge(Direction.Left, portrait, gaps))
        assertFalse(Workspace().hasEdge(Direction.Left, portrait, gaps))
    }

    @Test
    fun `in master a new tile becomes the master`() {
        val ws = Workspace(layout = TilingLayout.Master).insert(a).insert(b).insert(c)
        val rects = layout(ws, portrait, gaps).tiled
        assertEquals(Rect(2, 2, 998, 1098), rects[c])
        assertEquals(listOf(c, b, a), ws.root!!.leaves().map { it.shown })
        assertEquals(Workspace(layout = TilingLayout.Master).insert(a).insert(b), ws.remove(c))
    }
}
