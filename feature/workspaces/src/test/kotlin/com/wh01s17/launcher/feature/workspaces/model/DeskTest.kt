package com.wh01s17.launcher.feature.workspaces.model

import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.tiling.FloatingBounds
import com.wh01s17.launcher.core.tiling.Leaf
import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.core.tiling.TilingLayout
import com.wh01s17.launcher.core.tiling.Workspace
import com.wh01s17.launcher.core.tiling.toggleFloating
import com.wh01s17.launcher.core.tiling.withFloatingBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class DeskTest {
    private fun app(name: String) = TileContent.App(AppKey("com.$name", "com.$name.Main", 0))

    private val termux = app("termux")
    private val notes = app("notes")
    private val music = app("music")

    @Test
    fun `adding tiles gives fresh ids and fills the current workspace`() {
        val desk = Desk().add(termux).add(notes)
        assertEquals(listOf(TileId(1), TileId(2)), desk.currentWorkspace.tiles)
        assertEquals(mapOf(TileId(1) to termux, TileId(2) to notes), desk.contents)
        assertEquals(3, desk.nextId)
        assertEquals(setOf(1), desk.occupied)
        // Un id cerrado no se reutiliza.
        assertEquals(
            TileId(3),
            desk
                .close(TileId(2))
                .add(music)
                .currentWorkspace.tiles
                .last(),
        )
    }

    @Test
    fun `closing a tile drops its content and leaves other workspaces alone`() {
        val desk = Desk().add(termux).add(notes, workspace = 4)
        val closed = desk.close(TileId(1))
        assertEquals(setOf(4), closed.occupied)
        assertEquals(mapOf(TileId(2) to notes), closed.contents)
        assertSame(desk, desk.close(TileId(99)))
    }

    @Test
    fun `moving a tile follows it or not, and keeps it floating`() {
        val desk = Desk().add(termux).add(notes)
        val silent = desk.move(TileId(2), to = 3, follow = false)
        assertEquals(1, silent.current)
        assertEquals(listOf(TileId(2)), silent.workspace(3).tiles)
        assertEquals(TileId(2), silent.workspace(3).focused)
        assertEquals(3, desk.move(TileId(2), to = 3, follow = true).current)
        val bounds = FloatingBounds(0.1f, 0.1f, 0.5f, 0.3f)
        val floating = desk.updateCurrent { it.toggleFloating(TileId(1)).withFloatingBounds(TileId(1), bounds) }
        assertEquals(
            bounds,
            floating
                .move(TileId(1), to = 2, follow = false)
                .workspace(2)
                .floating
                .single()
                .bounds,
        )
        // Al mismo workspace no cambia nada (salvo ir a él).
        assertSame(desk, desk.move(TileId(1), to = 1, follow = false))
        assertEquals(1, desk.focusWorkspace(5).move(TileId(1), to = 1, follow = true).current)
        assertSame(desk, desk.move(TileId(9), to = 2, follow = true))
    }

    @Test
    fun `focus clamps and cycling visits occupied workspaces in order`() {
        assertEquals(10, Desk().focusWorkspace(12).current)
        assertEquals(1, Desk().focusWorkspace(0).current)
        val desk = Desk().add(termux, workspace = 2).add(notes, workspace = 7).focusWorkspace(2)
        assertEquals(7, desk.cycleWorkspace(forward = true).current)
        assertEquals(2, desk.cycleWorkspace(forward = true).cycleWorkspace(forward = true).current)
        assertEquals(7, desk.cycleWorkspace(forward = false).current)
        // Desde uno vacío también se puede salir.
        assertEquals(7, desk.focusWorkspace(4).cycleWorkspace(forward = true).current)
        assertEquals(1, Desk().cycleWorkspace(forward = true).current)
    }

    @Test
    fun `defaults set the tile limit everywhere and the layout only on empty workspaces`() {
        val desk = Desk().add(termux).withDefaults(TilingLayout.Master, maxTiles = 4)
        assertEquals(TilingLayout.Dwindle, desk.workspace(1).layout)
        assertEquals(TilingLayout.Master, desk.workspace(2).layout)
        assertEquals(List(Desk.COUNT) { 4 }, desk.workspaces.map { it.maxTiles })
        assertEquals(1, desk.workspaceOf(TileId(1)))
        assertNull(desk.workspaceOf(TileId(5)))
    }

    @Test
    fun `invalid desks are rejected`() {
        val one = Workspace(root = Leaf.of(TileId(1)))
        val ten = List(Desk.COUNT) { Workspace() }
        assertThrows(IllegalArgumentException::class.java) { Desk(workspaces = ten.drop(1)) }
        assertThrows(IllegalArgumentException::class.java) { Desk(current = 11) }
        assertThrows(IllegalArgumentException::class.java) { Desk(workspaces = listOf(one) + ten.drop(1)) }
        assertThrows(IllegalArgumentException::class.java) {
            Desk(workspaces = listOf(one, one) + ten.drop(2), contents = mapOf(TileId(1) to termux), nextId = 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Desk(workspaces = listOf(one) + ten.drop(1), contents = mapOf(TileId(1) to termux), nextId = 1)
        }
    }
}
