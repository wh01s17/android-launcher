package com.wh01s17.launcher.core.system.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentsClearGuardTest {
    private val launcher = "com.wh01s17.launcher.debug"
    private val instagram = "com.instagram.android"

    private fun guard() = RecentsClearGuard(launcher)

    @Test
    fun `clearing from home goes back home when HyperOS opens the kept app`() {
        val guard = guard()
        guard.onActiveApp(instagram, 0)
        guard.onActiveApp(launcher, 100)
        guard.onActiveApp(RecentsClearGuard.MIUI_HOME, 200)
        guard.onClearAll(300)
        assertTrue(guard.isPending(400))
        // La pantalla de recientes sigue a la vista un momento: no cuenta.
        assertFalse(guard.onActiveApp(RecentsClearGuard.MIUI_HOME, 500))
        assertTrue(guard.onActiveApp(instagram, 900))
        assertFalse(guard.isPending(950))
        // Sólo una vez: lo siguiente ya es uso normal.
        assertFalse(guard.onActiveApp(instagram, 1_000))
    }

    @Test
    fun `clearing from an app keeps that app`() {
        val guard = guard()
        guard.onActiveApp(instagram, 0)
        guard.onActiveApp(RecentsClearGuard.MIUI_HOME, 100)
        guard.onClearAll(200)
        assertFalse(guard.onActiveApp(instagram, 600))
    }

    @Test
    fun `opening an app from recents without clearing is left alone`() {
        val guard = guard()
        guard.onActiveApp(launcher, 0)
        guard.onActiveApp(RecentsClearGuard.MIUI_HOME, 100)
        assertFalse(guard.onActiveApp(instagram, 600))
    }

    @Test
    fun `the request expires and returning home cancels it`() {
        val late = guard()
        late.onActiveApp(launcher, 0)
        late.onClearAll(100)
        assertFalse(late.onActiveApp(instagram, 100 + RecentsClearGuard.DEFAULT_WINDOW_MILLIS + 1))

        val home = guard()
        home.onActiveApp(launcher, 0)
        home.onClearAll(100)
        assertFalse(home.onActiveApp(launcher, 400))
        assertFalse(home.onActiveApp(instagram, 500))
    }
}
