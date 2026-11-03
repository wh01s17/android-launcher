package com.wh01s17.launcher.core.apps.actions

import com.wh01s17.launcher.core.apps.model.AppKey
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutOrderTest {
    private val app = AppKey("p", "p.Main", 0)

    private fun shortcut(
        id: String,
        rank: Int,
        manifest: Boolean = false,
        enabled: Boolean = true,
    ) = AppShortcut(id, app, id, null, enabled, null, rank, manifest)

    @Test
    fun `manifest first, then dynamic, each by rank`() {
        val list = listOf(shortcut("d1", 1), shortcut("m1", 1, manifest = true), shortcut("d0", 0), shortcut("m0", 0, manifest = true))
        assertEquals(listOf("m0", "m1", "d0", "d1"), list.forMenu(10).map { it.id })
    }

    @Test
    fun `disabled go last and the limit applies`() {
        val list = listOf(shortcut("off", 0, manifest = true, enabled = false), shortcut("a", 0), shortcut("b", 1), shortcut("c", 2))
        assertEquals(listOf("a", "b", "c"), list.forMenu(3).map { it.id })
        assertEquals(listOf("a", "b", "c", "off"), list.forMenu(AppActions.MAX_MENU_SHORTCUTS).map { it.id })
    }

    @Test
    fun `duplicates and negative limits`() {
        assertEquals(listOf("a"), listOf(shortcut("a", 0), shortcut("a", 1)).forMenu(4).map { it.id })
        assertEquals(emptyList<AppShortcut>(), listOf(shortcut("a", 0)).forMenu(-1))
    }
}
