package com.wh01s17.launcher.feature.bar

import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.system.status.AudioState
import com.wh01s17.launcher.core.system.status.BatteryState
import com.wh01s17.launcher.core.system.status.NetworkKind
import com.wh01s17.launcher.core.system.status.NetworkState
import com.wh01s17.launcher.feature.bar.module.AudioModule
import com.wh01s17.launcher.feature.bar.module.NetworkModule
import com.wh01s17.launcher.feature.bar.module.PowerModule
import org.junit.Assert.assertEquals
import org.junit.Test

class BarStateTest {
    @Test
    fun `workspace ids are the persistent ones plus occupied and focused`() {
        assertEquals(listOf(1, 2, 3, 4, 5), WorkspaceBarState().ids)
        assertEquals(listOf(1, 2, 3, 4, 5, 9, 10), WorkspaceBarState(focused = 9, occupied = setOf(10, 1)).ids)
        assertEquals(listOf(1, 2, 3), WorkspaceBarState(persistent = 3, focused = 2).ids)
        // Fuera de 1..10 no se muestra.
        assertEquals(listOf(1, 2, 3, 4, 5), WorkspaceBarState(focused = 0, occupied = setOf(11, -1)).ids)
    }

    @Test
    fun `workspace 10 is called 0`() {
        assertEquals("0", WorkspaceBarState.label(10))
        assertEquals("7", WorkspaceBarState.label(7))
    }

    @Test
    fun `power classes and glyphs`() {
        val low = BatteryState(percent = 18, charging = false, full = false, powerSave = false)
        assertEquals(listOf("low"), PowerModule.classesOf(low))
        assertEquals(listOf("critical", "low"), PowerModule.classesOf(low.copy(percent = 8, powerSave = false)))
        assertEquals(
            listOf("charging", "power-save"),
            PowerModule.classesOf(BatteryState(50, charging = true, full = false, powerSave = true)),
        )
        assertEquals(listOf("charging", "full"), PowerModule.classesOf(BatteryState(100, charging = true, full = true, powerSave = false)))
        assertEquals(WhIcons.BatteryLow, PowerModule.glyphOf(low, 18))
        assertEquals(WhIcons.BatteryMedium, PowerModule.glyphOf(low.copy(percent = 55), 55))
        assertEquals(WhIcons.BatteryFull, PowerModule.glyphOf(low.copy(percent = 95), 95))
        assertEquals(WhIcons.Charging, PowerModule.glyphOf(low.copy(charging = true), 18))
    }

    @Test
    fun `network classes and glyphs`() {
        assertEquals(listOf("wifi"), NetworkModule.classesOf(NetworkState(NetworkKind.Wifi, hasInternet = true, vpn = false)))
        assertEquals(
            listOf("wifi", "no-internet", "vpn"),
            NetworkModule.classesOf(NetworkState(NetworkKind.Wifi, hasInternet = false, vpn = true)),
        )
        // Sin conexión no se dice «sin internet»: ya es `none`.
        assertEquals(listOf("none"), NetworkModule.classesOf(NetworkState.Offline))
        assertEquals(WhIcons.WifiOff, NetworkModule.glyphOf(NetworkKind.None))
        assertEquals(WhIcons.Cellular, NetworkModule.glyphOf(NetworkKind.Cellular))
    }

    @Test
    fun `audio classes`() {
        assertEquals("muted", AudioModule.classOf(AudioState(60, muted = true)))
        assertEquals("low", AudioModule.classOf(AudioState(49, muted = false)))
        assertEquals("high", AudioModule.classOf(AudioState(50, muted = false)))
        assertEquals(WhIcons.VolumeMute, AudioModule.glyphOf(AudioState(0, muted = true)))
    }
}
