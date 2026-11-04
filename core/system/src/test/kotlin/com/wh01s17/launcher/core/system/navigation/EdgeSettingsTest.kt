package com.wh01s17.launcher.core.system.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeSettingsTest {
    @Test
    fun `home cannot be turned off while the buttons are hidden`() {
        val settings = EdgeSettings(home = false, recents = false)
        assertTrue(settings.effective(buttonsHidden = true).home)
        assertEquals(false, settings.effective(buttonsHidden = false).home)
    }

    @Test
    fun `edges follow the enabled gestures and thickness is clamped`() {
        assertEquals(setOf(Edge.Right, Edge.Bottom), EdgeSettings(backLeft = false, home = false).edges)
        assertEquals(setOf(Edge.Left, Edge.Right), EdgeSettings(home = false, recents = false).edges)
        val clamped = EdgeSettings(sideThicknessDp = 2, bottomThicknessDp = 90).effective(buttonsHidden = false)
        assertEquals(8 to 32, clamped.sideThicknessDp to clamped.bottomThicknessDp)
    }
}
