package com.wh01s17.launcher.core.input

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LauncherCommandBusTest {
    @Test
    fun `a subscriber receives the commands in order`() =
        runTest(UnconfinedTestDispatcher()) {
            val bus = DefaultLauncherCommandBus()
            val received = mutableListOf<LauncherCommand>()
            backgroundScope.launch { bus.commands.collect { received += it } }
            bus.send(LauncherCommand.OpenMenu)
            bus.send(WorkspaceCommand.FocusWorkspace(3))
            assertEquals(listOf(LauncherCommand.OpenMenu, WorkspaceCommand.FocusWorkspace(3)), received)
        }

    @Test
    fun `commands sent without a subscriber are not replayed later`() =
        runTest(UnconfinedTestDispatcher()) {
            val bus = DefaultLauncherCommandBus()
            bus.send(LauncherCommand.OpenNotificationShade)
            val received = mutableListOf<LauncherCommand>()
            backgroundScope.launch { bus.commands.collect { received += it } }
            assertEquals(emptyList<LauncherCommand>(), received)
        }
}
