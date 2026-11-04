package com.wh01s17.launcher.feature.menu.search

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
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.fuzzy.Usage
import com.wh01s17.launcher.feature.menu.data.UsageRepository
import com.wh01s17.launcher.feature.menu.panel.MenuAction
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MenuSearchViewModelTest {
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
    private val history =
        object : UsageRepository {
            override val usage = MutableStateFlow(emptyMap<String, Usage>())
            var fail = false

            override suspend fun record(id: String) {
                if (fail) throw IOException("disk unavailable")
                val count = usage.value[id]?.launchCount ?: 0
                usage.value += id to Usage(count + 1, 1000)
            }
        }
    private val wifi = SearchTarget.Command(MenuAction.Wifi, "Wi-Fi", WhIcons.Wifi)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): MenuSearchViewModel =
        MenuSearchViewModel(repository, actions, history, listOf(wifi), dispatcher, { 1000 }, dispatcher).also { vm ->
            backgroundScope.launch { vm.state.collect {} }
            runCurrent()
            vm.onIntent(SearchIntent.Open)
            runCurrent()
        }

    @Test
    fun `global index contains apps commands and enabled shortcuts`() =
        runTest(dispatcher) {
            val vm = viewModel()
            assertTrue(
                vm.state.value.results
                    .any { it.target is SearchTarget.App },
            )
            assertTrue(
                vm.state.value.results
                    .any { it.target is SearchTarget.Command },
            )
            assertTrue(
                vm.state.value.results
                    .any { it.target is SearchTarget.Shortcut },
            )
            vm.onIntent(SearchIntent.QueryChanged("wifi"))
            runCurrent()
            assertEquals(
                listOf(wifi),
                vm.state.value.results
                    .map { it.target },
            )
        }

    @Test
    fun `no default home means no shortcuts and catalog changes rebuild index`() =
        runTest(dispatcher) {
            actions.shortcutsAllowed = false
            val vm = viewModel()
            assertFalse(
                vm.state.value.results
                    .any { it.target is SearchTarget.Shortcut },
            )
            repository.catalog.value = AppCatalog.of(listOf(personal), listOf(app("Nueva")))
            runCurrent()
            assertEquals(
                listOf("Nueva"),
                vm.state.value.results
                    .mapNotNull { (it.target as? SearchTarget.App)?.label },
            )
        }

    @Test
    fun `picker only includes apps and hands selection over without launch or ranking`() =
        runTest(dispatcher) {
            val vm = viewModel()
            val picked = mutableListOf<AppEntry>()
            backgroundScope.launch { vm.picks.collect { picked += it } }
            vm.onIntent(SearchIntent.OpenPicker)
            runCurrent()
            assertTrue(
                vm.state.value.results
                    .all { it.target is SearchTarget.App },
            )
            vm.onIntent(SearchIntent.LaunchFirst)
            runCurrent()
            assertEquals(1, picked.size)
            assertTrue(actions.launched.isEmpty())
            assertTrue(history.usage.value.isEmpty())
            assertFalse(vm.state.value.open)
        }

    @Test
    fun `selection wraps and query resets it`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(SearchIntent.MoveSelection(-1))
            runCurrent()
            assertEquals(vm.state.value.results.lastIndex, vm.state.value.selected)
            vm.onIntent(SearchIntent.QueryChanged("wifi"))
            runCurrent()
            assertEquals(0, vm.state.value.selected)
        }

    @Test
    fun `failed launch keeps query and does not rank, success ranks and closes`() =
        runTest(dispatcher) {
            val vm = viewModel()
            actions.result = LaunchResult.Failed("gone")
            vm.onIntent(SearchIntent.QueryChanged("zoom"))
            runCurrent()
            vm.onIntent(SearchIntent.Launch(app("Zoom")))
            runCurrent()
            assertTrue(vm.state.value.open)
            assertEquals("zoom", vm.state.value.query)
            assertTrue(history.usage.value.isEmpty())
            actions.result = LaunchResult.Launched
            vm.onIntent(SearchIntent.Launch(app("Zoom")))
            runCurrent()
            assertEquals(1, history.usage.value["app:${app("Zoom").key}"]?.launchCount)
            assertFalse(vm.state.value.open)
        }

    @Test
    fun `commands are ranked only after successful dispatch`() =
        runTest(dispatcher) {
            val vm = viewModel()
            val sent = mutableListOf<MenuAction>()
            backgroundScope.launch { vm.commands.collect { sent += it } }
            vm.onIntent(SearchIntent.Activate(wifi))
            runCurrent()
            assertEquals(listOf(MenuAction.Wifi), sent)
            assertTrue(history.usage.value.isEmpty())
            vm.commandFinished(MenuAction.Wifi, false)
            runCurrent()
            assertTrue(vm.state.value.open)
            vm.commandFinished(MenuAction.Wifi, true)
            runCurrent()
            assertEquals(1, history.usage.value[wifi.id]?.launchCount)
        }

    @Test
    fun `usage changes ranking without hiding matches`() =
        runTest(dispatcher) {
            val vm = viewModel()
            history.usage.value = mapOf(wifi.id to Usage(30, 1000))
            runCurrent()
            assertEquals(
                wifi,
                vm.state.value.results
                    .first()
                    .target,
            )
            assertTrue(vm.state.value.results.size > 1)
        }

    @Test
    fun `history write failure never prevents launching`() =
        runTest(dispatcher) {
            history.fail = true
            val vm = viewModel()
            vm.onIntent(SearchIntent.Launch(app("Zoom")))
            runCurrent()
            assertFalse(vm.state.value.open)
            assertFalse(vm.state.value.historyAvailable)
            assertEquals(listOf("Zoom"), actions.launched)
        }

    @Test
    fun `context menu still loads shortcuts and launches them`() =
        runTest(dispatcher) {
            val vm = viewModel()
            vm.onIntent(SearchIntent.ShowMenu(app("Calendario")))
            runCurrent()
            val shortcut =
                vm.state.value.menu!!
                    .shortcuts
                    .single()
            vm.onIntent(SearchIntent.LaunchShortcut(shortcut))
            runCurrent()
            assertEquals(listOf("shortcut:new"), actions.launched)
            assertTrue(
                history.usage.value.keys
                    .single()
                    .startsWith("shortcut:"),
            )
        }

    @Test
    fun `enter while search is pending waits for the matching query and executes once`() =
        runTest(dispatcher) {
            val vm = viewModel()
            val sent = mutableListOf<MenuAction>()
            backgroundScope.launch { vm.commands.collect { sent += it } }
            vm.onIntent(SearchIntent.QueryChanged("wifi"))
            vm.onIntent(SearchIntent.LaunchFirst)
            runCurrent()
            assertEquals(listOf(MenuAction.Wifi), sent)
            assertTrue(actions.launched.isEmpty())
        }

    @Test
    fun `editing cancels a pending activation rather than launching an old result`() =
        runTest(dispatcher) {
            val vm = viewModel()
            val sent = mutableListOf<MenuAction>()
            backgroundScope.launch { vm.commands.collect { sent += it } }
            vm.onIntent(SearchIntent.QueryChanged("wifi"))
            vm.onIntent(SearchIntent.LaunchFirst)
            vm.onIntent(SearchIntent.QueryChanged("Zoom"))
            runCurrent()
            assertTrue(sent.isEmpty())
            assertTrue(actions.launched.isEmpty())
        }
}
