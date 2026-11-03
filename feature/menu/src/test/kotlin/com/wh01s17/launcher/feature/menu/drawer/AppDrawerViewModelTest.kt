package com.wh01s17.launcher.feature.menu.drawer

import com.wh01s17.launcher.core.apps.AppRepository
import com.wh01s17.launcher.core.apps.actions.AppActions
import com.wh01s17.launcher.core.apps.actions.AppShortcut
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.actions.LaunchResult
import com.wh01s17.launcher.core.apps.model.AppCatalog
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.apps.model.AppProfile
import com.wh01s17.launcher.core.apps.model.ProfileKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class AppDrawerViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val personal = AppProfile(0, ProfileKind.Personal)
    private val work = AppProfile(10, ProfileKind.Work)

    private fun app(
        label: String,
        pkg: String = label.lowercase(),
        user: Long = 0,
        suspended: Boolean = false,
    ) = AppEntry(AppKey(pkg, "$pkg.Main", user), label, suspended)

    private val repository =
        object : AppRepository {
            override val catalog =
                MutableStateFlow(
                    AppCatalog.of(
                        listOf(personal, work.copy(available = false)),
                        listOf(app("Calculadora"), app("Calendario"), app("Teams", user = 10), app("Zoom", suspended = true)),
                    ),
                )
        }

    private class FakeActions : AppActions {
        var result: LaunchResult = LaunchResult.Launched
        val launched = mutableListOf<String>()
        var shortcutsAllowed = true

        override fun launch(
            app: AppEntry,
            origin: LaunchOrigin?,
        ) = result.also { launched += app.label }

        override fun openAppInfo(
            app: AppEntry,
            origin: LaunchOrigin?,
        ) = result.also { launched += "info:${app.label}" }

        override fun requestUninstall(app: AppEntry) = result.also { launched += "uninstall:${app.label}" }

        override fun canReadShortcuts() = shortcutsAllowed

        override fun shortcuts(
            app: AppEntry,
            limit: Int,
        ) = listOf(AppShortcut("new", app.key, "Nuevo", null, true, null, 0, true))

        override fun launchShortcut(
            shortcut: AppShortcut,
            origin: LaunchOrigin?,
        ) = result.also { launched += "shortcut:${shortcut.id}" }
    }

    private val actions = FakeActions()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): AppDrawerViewModel =
        AppDrawerViewModel(repository, actions, dispatcher).also { vm ->
            backgroundScope.launch { vm.state.collect {} }
            runCurrent()
        }

    @Test
    fun `lists everything and filters as you type`() =
        runTest(dispatcher) {
            val vm = viewModel()
            assertTrue(vm.state.value.loaded)
            assertEquals(4, vm.state.value.items.size)
            vm.onIntent(DrawerIntent.QueryChanged("cal"))
            runCurrent()
            assertEquals(
                listOf("Calculadora", "Calendario"),
                vm.state.value.items
                    .map { it.app.label },
            )
            assertEquals(
                listOf(0, 1, 2),
                vm.state.value.items
                    .first()
                    .matchPositions
                    .toList(),
            )
        }

    @Test
    fun `launching closes the drawer and clears the search`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(DrawerIntent.Open)
            vm.onIntent(DrawerIntent.QueryChanged("zo"))
            runCurrent()
            vm.onIntent(DrawerIntent.LaunchFirst)
            runCurrent()
            assertEquals(listOf("Zoom"), actions.launched)
            assertFalse(vm.state.value.open)
            assertEquals("", vm.state.value.query)
        }

    @Test
    fun `a failed launch keeps the drawer open with a message`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(DrawerIntent.Open)
            actions.result = LaunchResult.ProfilePaused(resumeRequested = true)
            vm.onIntent(DrawerIntent.Launch(app("Teams", user = 10)))
            runCurrent()
            assertTrue(vm.state.value.open)
            assertEquals(DrawerMessage.ProfileResumeRequested, vm.state.value.message)
            vm.onIntent(DrawerIntent.QueryChanged("t"))
            runCurrent()
            assertNull(vm.state.value.message)
            actions.result = LaunchResult.Failed("gone")
            vm.onIntent(DrawerIntent.Launch(app("Teams", user = 10)))
            runCurrent()
            assertEquals(DrawerMessage.LaunchFailed, vm.state.value.message)
        }

    @Test
    fun `badges show paused profiles, suspended apps and work apps`() =
        runTest(dispatcher) {
            val vm = viewModel()
            val badges =
                vm.state.value.items
                    .associate { it.app.label to it.badge }
            assertEquals(DrawerBadge.ProfilePaused, badges["Teams"])
            assertEquals(DrawerBadge.Suspended, badges["Zoom"])
            assertNull(badges["Calculadora"])
            repository.catalog.value = AppCatalog.of(listOf(personal, work), listOf(app("Teams", user = 10)))
            runCurrent()
            assertEquals(
                DrawerBadge.WorkProfile,
                vm.state.value.items
                    .single()
                    .badge,
            )
        }

    @Test
    fun `the menu loads shortcuts or explains why it cannot`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(DrawerIntent.ShowMenu(app("Calendario")))
            assertTrue(
                vm.state.value.menu
                    ?.loading ?: true,
            )
            runCurrent()
            val menu = vm.state.value.menu!!
            assertEquals(listOf("new"), menu.shortcuts.map { it.id })
            assertTrue(menu.shortcutsAvailable)
            actions.shortcutsAllowed = false
            vm.onIntent(DrawerIntent.ShowMenu(app("Calculadora")))
            runCurrent()
            assertFalse(
                vm.state.value.menu!!
                    .shortcutsAvailable,
            )
            assertTrue(
                vm.state.value.menu!!
                    .shortcuts
                    .isEmpty(),
            )
            vm.onIntent(DrawerIntent.DismissMenu)
            runCurrent()
            assertNull(vm.state.value.menu)
        }

    @Test
    fun `info, uninstall and shortcuts go to the actions`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(DrawerIntent.OpenInfo(app("Zoom")))
            vm.onIntent(DrawerIntent.Uninstall(app("Zoom")))
            vm.onIntent(DrawerIntent.LaunchShortcut(AppShortcut("new", app("Zoom").key, "Nuevo", null, true, null, 0, true)))
            runCurrent()
            assertEquals(listOf("info:Zoom", "uninstall:Zoom", "shortcut:new"), actions.launched)
        }

    @Test
    fun `close resets everything`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(DrawerIntent.Open)
            vm.onIntent(DrawerIntent.QueryChanged("x"))
            vm.onIntent(DrawerIntent.ShowMenu(app("Zoom")))
            runCurrent()
            vm.onIntent(DrawerIntent.Close)
            runCurrent()
            assertEquals(AppDrawerState(loaded = true, items = vm.state.value.items), vm.state.value)
        }
}
