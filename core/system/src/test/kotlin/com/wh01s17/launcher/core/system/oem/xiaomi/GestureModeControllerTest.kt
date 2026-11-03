package com.wh01s17.launcher.core.system.oem.xiaomi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** El SettingsGateway falso del plan (§7, Hito 2): guarda los valores y cuenta las escrituras. */
private class FakeSettings(
    var permission: Boolean = true,
) : SecureSettingsGateway {
    val values =
        mutableMapOf(
            SystemSetting.ForceFsgNavBar to 0,
            SystemSetting.HideGestureLine to 0,
            SystemSetting.NavigationMode to 0,
        )
    val writes = mutableListOf<Pair<SystemSetting, Int>>()

    override fun canWrite() = permission

    override fun read(setting: SystemSetting) = values[setting]

    override fun write(
        setting: SystemSetting,
        value: Int,
    ): Boolean {
        if (!permission) return false
        values[setting] = value
        writes += setting to value
        return true
    }
}

class GestureModeControllerTest {
    private val settings = FakeSettings()
    private var home = true
    private var now = 0L
    private val controller = GestureModeController(settings, { home }, { now })
    private val on = GestureModePrefs(enabled = true)
    private val fsg get() = settings.values[SystemSetting.ForceFsgNavBar]

    @Test
    fun `nothing is touched while the mode is off`() {
        assertEquals(GestureModeStatus.Off, controller.onNavigationReady(GestureModePrefs()))
        assertEquals(GestureModeStatus.Off, controller.onForceFsgChanged(GestureModePrefs()))
        assertFalse(controller.ensureSafe(GestureModePrefs(), navigationAlive = false))
        assertTrue(settings.writes.isEmpty())
    }

    @Test
    fun `buttons are hidden only when navigation is ready, we are home and allowed`() {
        home = false
        assertEquals(GestureModeStatus.NotHome, controller.onNavigationReady(on))
        home = true
        settings.permission = false
        assertEquals(GestureModeStatus.MissingPermission, controller.onNavigationReady(on))
        settings.permission = true
        assertEquals(GestureModeStatus.ButtonsHidden, controller.onNavigationReady(on))
        assertEquals(1, fsg)
        assertEquals(1, settings.values[SystemSetting.HideGestureLine])
    }

    @Test
    fun `the gesture line is left alone if the user wants it`() {
        controller.onNavigationReady(on.copy(hideGestureLine = false))
        assertEquals(0, settings.values[SystemSetting.HideGestureLine])
    }

    @Test
    fun `stopping navigation always gives the buttons back`() {
        controller.onNavigationReady(on)
        assertEquals(GestureModeStatus.Restored, controller.onNavigationStopping(on))
        assertEquals(0, fsg)
    }

    @Test
    fun `with the official launcher as home its gestures are never touched`() {
        settings.values[SystemSetting.ForceFsgNavBar] = 1
        home = false
        assertEquals(GestureModeStatus.Off, controller.onNavigationStopping(on))
        assertFalse(controller.ensureSafe(on, navigationAlive = false))
        controller.restoreAfterDisable(on)
        assertEquals(1, fsg)
    }

    @Test
    fun `a reset by HyperOS is undone, at most five times a minute`() {
        controller.onNavigationReady(on)
        repeat(GestureModeController.MAX_REWRITES_PER_MINUTE) {
            now += 1_000
            settings.values[SystemSetting.ForceFsgNavBar] = 0
            assertEquals(GestureModeStatus.ButtonsHidden, controller.onForceFsgChanged(on))
            assertEquals(1, fsg)
        }
        now += 1_000
        settings.values[SystemSetting.ForceFsgNavBar] = 0
        assertEquals(GestureModeStatus.GaveUp, controller.onForceFsgChanged(on))
        assertEquals(0, fsg)
        // Rendido, no insiste aunque pase el minuto, hasta que la navegación vuelve a arrancar.
        now += 120_000
        assertEquals(GestureModeStatus.GaveUp, controller.onForceFsgChanged(on))
        assertEquals(GestureModeStatus.ButtonsHidden, controller.onNavigationReady(on))
    }

    @Test
    fun `rewrites older than a minute stop counting`() {
        controller.onNavigationReady(on)
        repeat(GestureModeController.MAX_REWRITES_PER_MINUTE) {
            settings.values[SystemSetting.ForceFsgNavBar] = 0
            controller.onForceFsgChanged(on)
        }
        now += 60_000
        settings.values[SystemSetting.ForceFsgNavBar] = 0
        assertEquals(GestureModeStatus.ButtonsHidden, controller.onForceFsgChanged(on))
    }

    @Test
    fun `our own write of 1 is not a reset`() {
        controller.onNavigationReady(on)
        val before = settings.writes.size
        assertEquals(GestureModeStatus.ButtonsHidden, controller.onForceFsgChanged(on))
        assertEquals(before, settings.writes.size)
    }

    @Test
    fun `the watchdog restores buttons only without navigation`() {
        controller.onNavigationReady(on)
        assertFalse(controller.ensureSafe(on, navigationAlive = true))
        assertEquals(1, fsg)
        assertTrue(controller.ensureSafe(on, navigationAlive = false))
        assertEquals(0, fsg)
        // Ya estaban visibles: no hay nada que avisar.
        assertFalse(controller.ensureSafe(on, navigationAlive = false))
    }

    @Test
    fun `enable remembers the gesture line and disable restores it`() {
        settings.values[SystemSetting.HideGestureLine] = 0
        val enabled = controller.enable(GestureModePrefs())
        assertEquals(GestureModePrefs(enabled = true, previousGestureLine = 0), enabled)
        controller.onNavigationReady(enabled)
        assertEquals(1, settings.values[SystemSetting.HideGestureLine])
        val disabled = controller.disable(enabled)
        assertFalse(disabled.enabled)
        controller.restoreAfterDisable(enabled)
        assertEquals(0, fsg)
        assertEquals(0, settings.values[SystemSetting.HideGestureLine])
        // Activar dos veces no pisa el valor original guardado.
        settings.values[SystemSetting.HideGestureLine] = 1
        assertEquals(0, controller.enable(enabled).previousGestureLine)
    }
}
