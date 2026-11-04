package com.wh01s17.launcher.core.system.status

import android.app.NotificationManager
import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusStatesTest {
    private fun battery(
        level: Int,
        status: Int = BatteryManager.BATTERY_STATUS_DISCHARGING,
        plugged: Int = 0,
        scale: Int = 100,
        present: Boolean = true,
        powerSave: Boolean = false,
    ) = BatteryState.fromExtras(level, scale, status, plugged, present, powerSave)

    @Test
    fun `battery percent follows level over scale`() {
        assertEquals(50, battery(level = 125, scale = 250).percent)
        assertEquals(100, battery(level = 300, scale = 100).percent)
        assertNull(battery(level = -1).percent)
        assertNull(battery(level = 50, scale = 0).percent)
        assertNull(battery(level = 50, present = false).percent)
    }

    @Test
    fun `charging and full`() {
        assertTrue(battery(40, BatteryManager.BATTERY_STATUS_CHARGING, plugged = 1).charging)
        val full = battery(100, BatteryManager.BATTERY_STATUS_FULL, plugged = 2)
        assertTrue(full.full)
        assertTrue(full.charging)
        // Lleno y sin cargador: no está cargando.
        assertFalse(battery(100, BatteryManager.BATTERY_STATUS_FULL, plugged = 0).charging)
        assertFalse(battery(40).charging)
    }

    @Test
    fun `low and critical only apply off the charger`() {
        assertTrue(battery(20).low)
        assertFalse(battery(21).low)
        assertTrue(battery(10).critical)
        assertFalse(battery(11).critical)
        assertFalse(battery(5, BatteryManager.BATTERY_STATUS_CHARGING, plugged = 1).low)
        assertFalse(BatteryState.Unknown.low)
    }

    @Test
    fun `network kind prefers ethernet then wifi then cellular`() {
        assertEquals(NetworkKind.Ethernet, NetworkState.kindOf(wifi = true, cellular = true, ethernet = true))
        assertEquals(NetworkKind.Wifi, NetworkState.kindOf(wifi = true, cellular = true, ethernet = false))
        assertEquals(NetworkKind.Cellular, NetworkState.kindOf(wifi = false, cellular = true, ethernet = false))
        assertEquals(NetworkKind.Other, NetworkState.kindOf(wifi = false, cellular = false, ethernet = false))
    }

    @Test
    fun `audio percent and mute`() {
        assertEquals(AudioState(60, muted = false), AudioState.of(volume = 9, max = 15, streamMuted = false))
        assertTrue(AudioState.of(volume = 0, max = 15, streamMuted = false).muted)
        assertTrue(AudioState.of(volume = 9, max = 15, streamMuted = true).muted)
        assertEquals(AudioState(0, muted = true), AudioState.of(volume = 3, max = 0, streamMuted = false).copy(muted = true))
    }

    @Test
    fun `do not disturb filters`() {
        assertFalse(DoNotDisturbState.ofFilter(NotificationManager.INTERRUPTION_FILTER_ALL).active)
        assertFalse(DoNotDisturbState.ofFilter(NotificationManager.INTERRUPTION_FILTER_UNKNOWN).active)
        for (filter in listOf(
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
        )) {
            assertTrue(DoNotDisturbState.ofFilter(filter).active)
        }
    }

    @Test
    fun `clock ticks land on the next boundary`() {
        assertEquals(1_000, SystemClockSource.millisUntilNext(5_000, ClockGranularity.Second))
        assertEquals(1, SystemClockSource.millisUntilNext(5_999, ClockGranularity.Second))
        assertEquals(60_000, SystemClockSource.millisUntilNext(120_000, ClockGranularity.Minute))
        assertEquals(30_000, SystemClockSource.millisUntilNext(150_000, ClockGranularity.Minute))
        // Antes de 1970 también cae en el siguiente límite.
        assertEquals(500, SystemClockSource.millisUntilNext(-500, ClockGranularity.Second))
    }
}
