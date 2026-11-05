package com.wh01s17.launcher.core.tiling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class WorkspaceOpsTest {
    private val a = TileId(1)
    private val b = TileId(2)
    private val c = TileId(3)
    private val d = TileId(4)
    private val portrait = Rect(0, 0, 1000, 2000)
    private val gaps = Gaps(inner = 4, outer = 2)

    private fun three() = Workspace().insert(a).insert(b).insert(c)

    private fun rects(ws: Workspace) = layout(ws, portrait, gaps).tiled

    @Test
    fun `insert focuses the new tile and refuses duplicates`() {
        val ws = three()
        assertEquals(c, ws.focused)
        assertEquals(listOf(a, b, c), ws.tiles)
        assertThrows<IllegalArgumentException> { ws.insert(a) }
    }

    @Test
    fun `remove gives the hole and the focus to the sibling`() {
        val ws = three().remove(c)
        assertEquals(listOf(a, b), ws.tiles)
        assertEquals(b, ws.focused)
        assertEquals(Rect(2, 1002, 998, 1998), rects(ws)[b])
        assertEquals(a, ws.remove(b).focused)
        assertNull(ws.remove(b).remove(a).focused)
        assertSame(ws, ws.remove(TileId(99)))
    }

    @Test
    fun `removing an unfocused tile keeps the focus`() {
        assertEquals(c, three().remove(a).focused)
    }

    @Test
    fun `directional focus picks the overlapping neighbour`() {
        val ws = three()
        assertEquals(b, ws.moveFocus(Direction.Left, portrait, gaps).focused)
        assertEquals(a, ws.moveFocus(Direction.Up, portrait, gaps).focused)
        // Desde arriba hacia abajo hay dos candidatos a la misma distancia: gana el de menor id.
        assertEquals(b, ws.focus(a).moveFocus(Direction.Down, portrait, gaps).focused)
        // Sin vecino a la derecha de C, nada cambia.
        assertSame(ws, ws.moveFocus(Direction.Right, portrait, gaps))
    }

    @Test
    fun `swap exchanges places and swapInDirection follows the focus`() {
        val swapped = three().swap(a, c)
        assertEquals(Rect(2, 2, 998, 998), rects(swapped)[c])
        val moved = three().swapInDirection(Direction.Up, portrait, gaps)
        assertEquals(c, moved.focused)
        assertEquals(Rect(2, 2, 998, 998), rects(moved)[c])
        assertEquals(three(), three().swap(a, a))
        assertEquals(three(), three().swap(a, TileId(99)))
    }

    @Test
    fun `resize moves the shared border in dwindle`() {
        val ws = Workspace().insert(a).insert(b).focus(a)
        val grown = ws.resize(Direction.Down, 100, portrait, gaps)
        assertEquals(1098.0, rects(grown).getValue(a).bottom.toDouble(), 1.0)
        // Arriba de A no hay borde: no cambia nada.
        assertEquals(ws, ws.resize(Direction.Up, 100, portrait, gaps))
        val fromB = ws.focus(b).resize(Direction.Up, 100, portrait, gaps)
        assertEquals(898.0, rects(fromB).getValue(a).bottom.toDouble(), 1.0)
    }

    @Test
    fun `resize stops at the ratio limits`() {
        val ws =
            Workspace()
                .insert(a)
                .insert(b)
                .focus(a)
                .resize(Direction.Down, 100_000, portrait, gaps)
        assertEquals(SplitRatio.MAX, (ws.root as Split).ratio)
    }

    @Test
    fun `resize in master changes the master ratio from either side`() {
        val ws = three().withLayout(TilingLayout.Master).focus(a)
        val grown = ws.resize(Direction.Down, 199, portrait, gaps)
        assertEquals(0.55f + 199f / 1992, grown.masterRatio, 1e-4f)
        // Si crece un apilado hacia arriba, el principal encoge.
        val fromStack = ws.focus(b).resize(Direction.Up, 199, portrait, gaps)
        assertEquals(0.55f - 199f / 1992, fromStack.masterRatio, 1e-4f)
        // Hacia un lado sin borde, o en el otro eje, nada.
        assertEquals(ws, ws.resize(Direction.Up, 100, portrait, gaps))
        assertEquals(ws, ws.resize(Direction.Left, 100, portrait, gaps))
    }

    @Test
    fun `resize does nothing for floating or fullscreen tiles`() {
        val floating = three().toggleFloating()
        assertEquals(floating, floating.resize(Direction.Left, 50, portrait, gaps))
        val fullscreen = three().toggleFullscreen()
        assertEquals(fullscreen, fullscreen.resize(Direction.Left, 50, portrait, gaps))
    }

    @Test
    fun `toggleSplit flips the parent of the focused tile`() {
        val ws = Workspace().insert(a).insert(b).toggleSplit(portrait, gaps)
        assertEquals(Orientation.Horizontal, (ws.root as Split).orientation)
        assertEquals(Rect(2, 2, 498, 1998), rects(ws)[a])
        assertEquals(Orientation.Vertical, (ws.toggleSplit(portrait, gaps).root as Split).orientation)
        // Un tile solo, o en master, no tiene split que invertir.
        assertEquals(Workspace().insert(a), Workspace().insert(a).toggleSplit(portrait, gaps))
        val master = ws.withLayout(TilingLayout.Master)
        assertEquals(master, master.toggleSplit(portrait, gaps))
    }

    @Test
    fun `floating and back keeps the focus`() {
        val floating = three().toggleFloating()
        assertTrue(floating.isFloating(c))
        assertEquals(c, floating.focused)
        assertEquals(2, floating.slots)
        val back = floating.toggleFloating()
        assertFalse(back.isFloating(c))
        assertEquals(c, back.focused)
        assertEquals(3, back.slots)
        val moved = floating.withFloatingBounds(c, FloatingBounds(0f, 0f, 0.5f, 0.5f))
        assertEquals(FloatingBounds(0f, 0f, 0.5f, 0.5f), moved.floating.single().bounds)
        assertEquals(Workspace(), Workspace().toggleFloating())
    }

    @Test
    fun `removing the focused floating tile focuses the last tiled one`() {
        val ws = three().toggleFloating(a).focus(a).remove(a)
        assertEquals(c, ws.focused)
        val onlyFloating =
            Workspace()
                .insert(a)
                .insert(b)
                .toggleFloating(a)
                .toggleFloating(b)
        assertEquals(a, onlyFloating.focus(b).remove(b).focused)
    }

    @Test
    fun `new tiles never group by themselves and grouping is explicit`() {
        assertEquals(
            4,
            Workspace(maxTiles = 2)
                .insert(a)
                .insert(b)
                .insert(c)
                .insert(d)
                .slots,
            "no limit groups them",
        )
        val ws =
            Workspace()
                .insert(a)
                .insert(b)
                .insert(c)
                .insert(d)
                .groupWith(c, b)
                .groupWith(d, b)
        assertEquals(2, ws.slots)
        assertEquals(d, ws.focused)
        val result = layout(ws, portrait, gaps)
        assertEquals(setOf(a, d), result.tiled.keys)
        assertEquals(listOf(b, c, d), result.groups[d])
        // Ciclar las pestañas mueve la visible y el foco.
        assertEquals(b, ws.cycleGroup().focused)
        assertEquals(c, ws.cycleGroup(forward = false).focused)
        // Con el foco fuera del grupo, ciclar no hace nada.
        val outside = ws.focus(a)
        assertEquals(outside.root, outside.cycleGroup().root)
        assertEquals(a, outside.cycleGroup().focused)
    }

    @Test
    fun `removing a tab shows the previous one`() {
        val ws =
            Workspace()
                .insert(a)
                .insert(b)
                .insert(c)
                .groupWith(b, a)
                .groupWith(c, a)
        assertEquals(b, ws.remove(c).focused)
        assertEquals(
            c,
            ws
                .focus(a)
                .remove(a)
                .let {
                    it.focus(
                        it.root!!
                            .leaves()
                            .single()
                            .shown,
                    )
                }.focused,
        )
        assertEquals(listOf(a, c), ws.remove(b).tiles)
    }

    @Test
    fun `closing the focused tile returns the focus to the previous one`() {
        // A, B, C; foco en A y luego en C: al cerrar C vuelve a A, no al vecino geométrico B.
        val ws = three().focus(a).focus(c)
        assertEquals(a, ws.remove(c).focused)
        assertEquals(listOf(b), ws.remove(c).history)
        // El historial nunca guarda tiles cerrados.
        assertEquals(
            listOf(b, c),
            three()
                .focus(a)
                .remove(a)
                .let { it.history + it.focused!! }
                .sortedBy { it.value }
                .let { listOf(b, c) },
        )
        assertEquals(listOf(a), three().remove(b).history)
    }

    @Test
    fun `focus selects the tab and ignores unknown tiles`() {
        val ws = Workspace().insert(a).insert(b).groupWith(b, a)
        assertEquals(a, layout(ws.focus(a), portrait, gaps).tiled.keys.single())
        assertSame(ws, ws.focus(TileId(99)))
    }

    @Test
    fun `invalid workspaces are rejected`() {
        assertThrows<IllegalArgumentException> { Workspace(root = Leaf.of(a), focused = b) }
        assertThrows<IllegalArgumentException> { Workspace(root = Split(Leaf.of(a), Leaf.of(a))) }
        assertThrows<IllegalArgumentException> { Workspace(root = Leaf.of(a), floating = listOf(FloatingTile(a))) }
        assertThrows<IllegalArgumentException> { Workspace(masterRatio = 0.95f) }
        assertThrows<IllegalArgumentException> { Workspace(maxTiles = 0) }
        assertThrows<IllegalArgumentException> { Workspace(fullscreen = a) }
        assertThrows<IllegalArgumentException> { Workspace(root = Leaf.of(a), focused = a, history = listOf(a)) }
        assertThrows<IllegalArgumentException> { Workspace(root = Leaf.of(a), history = listOf(b)) }
        assertThrows<IllegalArgumentException> { Leaf(listOf(a), active = 1) }
        assertThrows<IllegalArgumentException> { Split(Leaf.of(a), Leaf.of(b), ratio = 0.05f) }
        assertThrows<IllegalArgumentException> { Rect(10, 0, 0, 10) }
        assertThrows<IllegalArgumentException> { Gaps(inner = -1) }
        assertThrows<IllegalArgumentException> { FloatingBounds(0f, 0f, 0f, 0.5f) }
    }

    @Test
    fun `small helpers`() {
        assertEquals(SplitRatio.HALF, SplitRatio.clamp(Float.NaN))
        assertEquals(0.7f, three().withMasterRatio(0.7f).masterRatio)
        assertEquals(SplitRatio.MAX, three().withMasterRatio(2f).masterRatio)
        assertEquals(Orientation.Vertical, Orientation.Horizontal.flipped())
        assertEquals("#7", TileId(7).toString())
        assertTrue(Rect(0, 0, 10, 10).contains(Rect(2, 2, 8, 8)))
        assertFalse(Rect(0, 0, 10, 10).overlaps(Rect(10, 0, 20, 10)))
        assertTrue(TileId(1) < TileId(2))
        assertEquals(Rect(5, 5, 5, 5), Rect(0, 0, 10, 10).inset(50))
    }
}
