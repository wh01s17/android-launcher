package com.wh01s17.launcher.feature.settings.compat

import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilityGateway
import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilitySnapshot
import com.wh01s17.launcher.core.system.oem.xiaomi.DeviceInfo
import com.wh01s17.launcher.core.system.oem.xiaomi.GestureModePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompatibilityViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeGateway : CompatibilityGateway {
        var snapshot =
            CompatibilitySnapshot(
                packageName = "com.wh01s17.launcher",
                device = DeviceInfo("Xiaomi", "Redmi", "2312DRA50G", 35, "OS2.0.204.0.VNRMIXM"),
                serviceEnabled = true,
                serviceRunning = true,
                permissionGranted = true,
                isDefaultHome = true,
                forceFsgNavBar = 0,
                navigationMode = 0,
                prefs = GestureModePrefs(),
                status = null,
                lastRestoreAtMillis = null,
            )
        val modeCalls = mutableListOf<Boolean>()
        var onChange: (() -> Unit)? = null
        var closed = false

        override fun snapshot() = snapshot

        override fun setModeEnabled(enabled: Boolean) {
            modeCalls += enabled
            snapshot = snapshot.copy(prefs = snapshot.prefs.copy(enabled = enabled), forceFsgNavBar = if (enabled) 1 else 0)
        }

        override fun observe(onChange: () -> Unit): AutoCloseable {
            this.onChange = onChange
            return AutoCloseable { closed = true }
        }
    }

    private val gateway = FakeGateway()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `reads the state off the main thread and refreshes on changes`() =
        runTest(dispatcher) {
            val vm = CompatibilityViewModel(gateway, dispatcher)
            assertNull(vm.state.value.snapshot)
            runCurrent()
            assertEquals(
                0,
                vm.state.value.snapshot
                    ?.forceFsgNavBar,
            )
            gateway.snapshot = gateway.snapshot.copy(forceFsgNavBar = 1)
            gateway.onChange!!.invoke()
            runCurrent()
            assertEquals(
                1,
                vm.state.value.snapshot
                    ?.forceFsgNavBar,
            )
        }

    @Test
    fun `the mode needs the service and the permission to be turned on`() =
        runTest(dispatcher) {
            gateway.snapshot = gateway.snapshot.copy(permissionGranted = false)
            val vm = CompatibilityViewModel(gateway, dispatcher)
            runCurrent()
            assertFalse(vm.state.value.modeToggleEnabled)
            vm.setModeEnabled(true)
            assertTrue(gateway.modeCalls.isEmpty())
        }

    @Test
    fun `the mode can always be turned off`() =
        runTest(dispatcher) {
            gateway.snapshot = gateway.snapshot.copy(serviceRunning = false, prefs = GestureModePrefs(enabled = true))
            val vm = CompatibilityViewModel(gateway, dispatcher)
            runCurrent()
            assertTrue(vm.state.value.modeToggleEnabled)
            assertTrue(vm.state.value.serviceStuck)
            vm.setModeEnabled(false)
            runCurrent()
            assertEquals(listOf(false), gateway.modeCalls)
            assertFalse(
                vm.state.value.snapshot!!
                    .prefs.enabled,
            )
        }

    @Test
    fun `turning the mode on goes to the gateway and refreshes`() =
        runTest(dispatcher) {
            val vm = CompatibilityViewModel(gateway, dispatcher)
            runCurrent()
            vm.setModeEnabled(true)
            runCurrent()
            assertEquals(listOf(true), gateway.modeCalls)
            assertEquals(
                1,
                vm.state.value.snapshot
                    ?.forceFsgNavBar,
            )
        }

    @Test
    fun `a disabled service is not stuck`() {
        val state = CompatibilityState(gateway.snapshot.copy(serviceEnabled = false, serviceRunning = false))
        assertFalse(state.serviceStuck)
        assertFalse(state.modeToggleEnabled)
        assertFalse(CompatibilityState().modeToggleEnabled)
    }
}
