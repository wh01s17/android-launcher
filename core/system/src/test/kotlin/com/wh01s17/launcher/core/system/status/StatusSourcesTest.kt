package com.wh01s17.launcher.core.system.status

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.ZoneId

/** El plumbing de las fuentes con difusiones reales de Robolectric. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StatusSourcesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun batteryIntent(level: Int) =
        Intent(Intent.ACTION_BATTERY_CHANGED)
            .putExtra(BatteryManager.EXTRA_LEVEL, level)
            .putExtra(BatteryManager.EXTRA_SCALE, 100)
            .putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_DISCHARGING)
            .putExtra(BatteryManager.EXTRA_PRESENT, true)

    @Test
    fun `battery source emits on each change and ignores repeats`() =
        runTest(UnconfinedTestDispatcher()) {
            val seen = mutableListOf<Int?>()
            val job = backgroundScope.launch { BatterySource(context).observe().collect { seen += it.percent } }
            idle()
            context.sendBroadcast(batteryIntent(80))
            idle()
            context.sendBroadcast(batteryIntent(80))
            context.sendBroadcast(batteryIntent(79))
            idle()
            job.cancel()
            // El primer valor viene del sticky actual (o de ningún extra); después, sólo los cambios.
            assertEquals(listOf(80, 79), seen.filterNotNull())
        }

    @Test
    fun `a source stops listening when nobody collects`() =
        runTest(UnconfinedTestDispatcher()) {
            val seen = mutableListOf<Int?>()
            val job = backgroundScope.launch { BatterySource(context).observe().collect { seen += it.percent } }
            idle()
            context.sendBroadcast(batteryIntent(60))
            idle()
            job.cancel()
            idle()
            context.sendBroadcast(batteryIntent(50))
            idle()
            assertEquals(listOf(60), seen.filterNotNull())
        }

    @Test
    fun `do not disturb source follows the interruption filter`() =
        runTest(UnconfinedTestDispatcher()) {
            val seen = mutableListOf<Boolean>()
            val notifications = context.getSystemService(NotificationManager::class.java)
            val job = backgroundScope.launch { DoNotDisturbSource(context).observe().collect { seen += it.active } }
            idle()
            shadowOf(notifications).setNotificationPolicyAccessGranted(true)
            notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            context.sendBroadcast(Intent(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED))
            idle()
            job.cancel()
            assertEquals(listOf(false, true), seen)
        }

    @Test
    fun `clock source emits once at start and then on each boundary`() =
        runTest {
            // La hora corre con el reloj virtual de la prueba, empezando 500 ms antes del minuto.
            val source = SystemClockSource(context, nowMillis = { 59_500L + testScheduler.currentTime }, zone = { ZoneId.of("UTC") })
            val ticks =
                source
                    .ticks(ClockGranularity.Minute)
                    .take(3)
                    .toList()
                    .map { it.toInstant().toEpochMilli() }
            assertEquals(listOf(59_500L, 60_000L, 120_000L), ticks)
        }

    @Test
    fun `clock source also emits when the time changes`() =
        runTest(UnconfinedTestDispatcher()) {
            var now = 10_000L
            val source = SystemClockSource(context, nowMillis = { now }, zone = { ZoneId.of("UTC") })
            val seen = mutableListOf<Long>()
            val job = backgroundScope.launch { source.ticks(ClockGranularity.Minute).collect { seen += it.toInstant().toEpochMilli() } }
            idle()
            now = 500_000L
            context.sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED))
            idle()
            job.cancel()
            assertEquals(listOf(10_000L, 500_000L), seen)
        }
}
