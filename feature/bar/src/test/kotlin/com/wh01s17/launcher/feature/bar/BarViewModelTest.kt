package com.wh01s17.launcher.feature.bar

import com.wh01s17.launcher.core.input.DefaultLauncherCommandBus
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.system.notifications.NotificationSummary
import com.wh01s17.launcher.core.system.status.AudioState
import com.wh01s17.launcher.core.system.status.BatteryState
import com.wh01s17.launcher.core.system.status.BluetoothStatus
import com.wh01s17.launcher.core.system.status.DoNotDisturbState
import com.wh01s17.launcher.core.system.status.NetworkKind
import com.wh01s17.launcher.core.system.status.NetworkState
import com.wh01s17.launcher.core.system.status.StatusSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BarViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    /** Una fuente falsa que cuenta cuántos recolectores tiene. */
    private class FakeSource<T>(
        initial: T,
    ) : StatusSource<T> {
        val value = MutableStateFlow(initial)
        var collectors = 0
            private set

        override fun observe() = value.onStart { collectors++ }.onCompletion { collectors-- }
    }

    private val battery = FakeSource(BatteryState(80, charging = false, full = false, powerSave = false))
    private val network = FakeSource(NetworkState(NetworkKind.Wifi, hasInternet = true, vpn = false))
    private val bluetooth = FakeSource(BluetoothStatus.On)
    private val audio = FakeSource(AudioState(40, muted = false))
    private val dnd = FakeSource(DoNotDisturbState(false))
    private val notifications = FakeSource(NotificationSummary())
    private val workspaces = MutableStateFlow(WorkspaceBarState())
    private val notices = MutableStateFlow(emptyList<BarNotice>())
    private val bus = DefaultLauncherCommandBus()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = BarViewModel(BarSources(battery, network, bluetooth, audio, dnd, notifications), workspaces, notices, bus)

    @Test
    fun `the state joins every source and follows their changes`() =
        runTest(dispatcher) {
            val vm = viewModel()
            backgroundScope.launch { vm.state.collect {} }
            runCurrent()
            assertEquals(80, vm.state.value.battery.percent)
            assertEquals(NetworkKind.Wifi, vm.state.value.network.kind)
            assertEquals(BluetoothStatus.On, vm.state.value.bluetooth)
            assertEquals(40, vm.state.value.audio.volumePercent)
            battery.value.value = BatteryState(79, charging = true, full = false, powerSave = false)
            dnd.value.value = DoNotDisturbState(true)
            workspaces.value = WorkspaceBarState(focused = 3)
            notices.value = listOf(BarNotice("config", "config", NoticeSeverity.Error))
            runCurrent()
            assertEquals(79, vm.state.value.battery.percent)
            assertTrue(vm.state.value.doNotDisturb.active)
            assertEquals(3, vm.state.value.workspaces.focused)
            assertEquals(
                listOf("config"),
                vm.state.value.notices
                    .map { it.id },
            )
        }

    @Test
    fun `sources are only listened to while the ui collects and for a grace period after`() =
        runTest(dispatcher) {
            val vm = viewModel()
            assertEquals(0, battery.collectors)
            val job = backgroundScope.launch { vm.state.collect {} }
            runCurrent()
            assertEquals(1, battery.collectors)
            assertEquals(1, network.collectors)
            job.cancel()
            advanceTimeBy(4_000)
            assertEquals(1, battery.collectors) // el giro de pantalla no reinicia las fuentes
            advanceTimeBy(2_000)
            runCurrent()
            assertEquals(0, battery.collectors)
            assertEquals(0, audio.collectors)
        }

    @Test
    fun `commands go to the bus`() =
        runTest(dispatcher) {
            val received = mutableListOf<LauncherCommand>()
            backgroundScope.launch { bus.commands.collect { received += it } }
            runCurrent()
            viewModel().send(LauncherCommand.OpenMenu)
            runCurrent()
            assertEquals(listOf<LauncherCommand>(LauncherCommand.OpenMenu), received)
        }
}
