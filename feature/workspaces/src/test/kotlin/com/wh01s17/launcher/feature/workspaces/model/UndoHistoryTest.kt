package com.wh01s17.launcher.feature.workspaces.model

import com.wh01s17.launcher.core.apps.model.AppKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UndoHistoryTest {
    private var now = 0L
    private val history = UndoHistory(limit = 3) { now }
    private val termux = TileContent.App(AppKey("com.termux", "com.termux.Main", 0))
    private val notes = TileContent.App(AppKey("com.notes", "com.notes.Main", 0))

    @Test
    fun `layout changes are steps and focus changes are not`() {
        val empty = Desk()
        val one = empty.add(termux)
        history.record(empty, one)
        assertEquals(1, history.size)
        // Cambiar de escritorio o de foco no se deshace.
        history.record(one, one.focusWorkspace(3))
        assertEquals(1, history.size)
        assertEquals(empty, history.pop())
        assertNull(history.pop())
    }

    @Test
    fun `steps of one gesture count once until a pause`() {
        val a = Desk()
        val b = a.add(termux)
        val c = b.add(notes)
        history.record(a, b, key = "divider")
        now += 100
        history.record(b, c, key = "divider")
        assertEquals(1, history.size)
        now += 2_000
        history.record(c, c.close(c.currentWorkspace.tiles.first()), key = "divider")
        assertEquals(2, history.size)
    }

    @Test
    fun `only the last steps are kept`() {
        var desk = Desk()
        repeat(5) {
            val next = desk.add(termux)
            history.record(desk, next)
            desk = next
        }
        assertEquals(3, history.size)
    }
}
