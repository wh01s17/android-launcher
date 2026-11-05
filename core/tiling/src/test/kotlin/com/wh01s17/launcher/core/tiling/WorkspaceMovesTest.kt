package com.wh01s17.launcher.core.tiling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkspaceMovesTest {
    private val a = TileId(1)
    private val b = TileId(2)
    private val c = TileId(3)
    private val d = TileId(4)
    private val portrait = Rect(0, 0, 1000, 2000)
    private val gaps = Gaps(0, 0)

    /** Dwindle en vertical: a arriba; abajo b a la izquierda y c a la derecha. */
    private fun three() = Workspace().insert(a).insert(b).insert(c)

    @Test
    fun `dropping below a tile puts it there instead of swapping`() {
        val before = layout(three(), portrait, gaps).tiled
        val ws = three().moveNextTo(b, c, Direction.Down)
        val after = layout(ws, portrait, gaps).tiled
        // c se queda en su columna, arriba; b debajo de c. a no se mueve.
        assertEquals(before.getValue(a), after.getValue(a))
        assertEquals(after.getValue(c).left, after.getValue(b).left)
        assertEquals(after.getValue(c).right, after.getValue(b).right)
        assertTrue(after.getValue(c).bottom <= after.getValue(b).top)
        assertEquals(b, ws.focused)
    }

    @Test
    fun `each side splits the target that way`() {
        val left = layout(three().moveNextTo(a, c, Direction.Left), portrait, gaps).tiled
        assertTrue(left.getValue(a).right <= left.getValue(c).left)
        val up = layout(three().moveNextTo(a, c, Direction.Up), portrait, gaps).tiled
        assertTrue(up.getValue(a).bottom <= up.getValue(c).top)
        val right = layout(three().moveNextTo(c, a, Direction.Right), portrait, gaps).tiled
        assertTrue(right.getValue(a).right <= right.getValue(c).left)
    }

    @Test
    fun `a tab leaves its group and a group target keeps its tabs`() {
        val grouped =
            Workspace()
                .insert(a)
                .insert(b)
                .insert(c)
                .groupWith(c, b)
        val out = grouped.moveNextTo(c, b, Direction.Down)
        assertEquals(3, out.slots)
        val next =
            Workspace()
                .insert(a)
                .insert(b)
                .insert(c)
                .insert(d)
                .groupWith(c, b)
        val moved = next.moveNextTo(d, c, Direction.Right)
        assertEquals(listOf(b, c), moved.root!!.leafOf(c)!!.tiles)
        assertEquals(3, moved.slots)
    }

    @Test
    fun `nothing changes for the same tile or unknown ones`() {
        val ws = three()
        assertSame(ws, ws.moveNextTo(a, a, Direction.Down))
        assertSame(ws, ws.moveNextTo(TileId(9), a, Direction.Down))
        assertSame(ws, ws.moveNextTo(a, TileId(9), Direction.Down))
    }

    @Test
    fun `in master it lands right before or after the target`() {
        val ws =
            Workspace(layout = TilingLayout.Master)
                .insert(a)
                .insert(b)
                .insert(c)
                .insert(d)
        val order = ws.root!!.leaves().map { it.shown }
        val moved =
            ws
                .moveNextTo(order.first(), order.last(), Direction.Down)
                .root!!
                .leaves()
                .map { it.shown }
        assertEquals(order.drop(1) + order.first(), moved)
    }
}
