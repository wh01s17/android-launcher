package com.wh01s17.launcher.core.tiling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class WorkspaceGroupsTest {
    private val a = TileId(1)
    private val b = TileId(2)
    private val c = TileId(3)
    private val portrait = Rect(0, 0, 1000, 2000)
    private val gaps = Gaps(4, 2)

    private fun three() = Workspace().insert(a).insert(b).insert(c)

    @Test
    fun `group with moves a tile into another slot as the visible tab`() {
        val ws = three().groupWith(c, a)
        assertEquals(2, ws.slots)
        assertEquals(c, ws.focused)
        assertEquals(listOf(a, c), layout(ws, portrait, gaps).groups[c])
        assertSame(ws, ws.groupWith(a, c))
        assertSame(ws, ws.groupWith(TileId(9), a))
    }

    @Test
    fun `move out of group gives the tile its own slot next to the group`() {
        val grouped = three().groupWith(c, a)
        val out = grouped.moveOutOfGroup()
        assertEquals(3, out.slots)
        assertEquals(c, out.focused)
        assertSame(out, out.moveOutOfGroup())
    }

    @Test
    fun `toggle group splits a group or joins the previously focused tile`() {
        val joined = three().focus(a).focus(b).toggleGroup()
        assertEquals(2, joined.slots)
        assertEquals(listOf(a, b), joined.root!!.leafOf(b)!!.tiles)
        val split = joined.toggleGroup()
        assertEquals(3, split.slots)
        val alone = Workspace().insert(a)
        assertSame(alone, alone.toggleGroup(), "nobody to join")
    }

    @Test
    fun `move into group uses the neighbour in that direction`() {
        // Dwindle en vertical: a arriba, b abajo (y c dentro de la mitad de abajo).
        val ws =
            Workspace()
                .insert(a)
                .insert(b)
                .focus(b)
                .moveIntoGroup(Direction.Up, portrait, gaps)
        assertEquals(1, ws.slots)
        assertEquals(listOf(a, b), ws.root!!.leafOf(b)!!.tiles)
        assertSame(ws, ws.moveIntoGroup(Direction.Down, portrait, gaps))
    }
}
