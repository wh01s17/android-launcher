package com.wh01s17.launcher.feature.menu.panel

import com.wh01s17.launcher.feature.menu.R
import org.junit.Assert.assertEquals
import org.junit.Test

class MenuCatalogTest {
    @Test
    fun `every supported action is reachable from the unified command catalog`() {
        val commands =
            searchCommands {
                when (it) {
                    R.string.menu_go_workspace -> "Ir al escritorio %1\$d"
                    R.string.menu_move_workspace -> "Mover tile al escritorio %1\$d"
                    else -> "resource:$it"
                }
            }
        assertEquals(MenuAction.entries.toSet(), commands.map { it.action }.toSet())
        assertEquals(commands.size, commands.map { it.id }.distinct().size)
    }
}
