package com.wh01s17.launcher.feature.workspaces

import com.wh01s17.launcher.core.apps.actions.AppActions
import com.wh01s17.launcher.core.apps.actions.AppShortcut
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.actions.LaunchResult
import com.wh01s17.launcher.core.apps.model.AppCatalog
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.apps.model.AppProfile
import com.wh01s17.launcher.core.apps.model.ProfileKind
import com.wh01s17.launcher.core.config.BarConfig
import com.wh01s17.launcher.core.config.GeneralConfig
import com.wh01s17.launcher.core.config.LauncherConfig
import com.wh01s17.launcher.core.input.CommandDirection
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.tiling.Direction
import com.wh01s17.launcher.core.tiling.FloatingBounds
import com.wh01s17.launcher.core.tiling.Gaps
import com.wh01s17.launcher.core.tiling.Rect
import com.wh01s17.launcher.core.tiling.Split
import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.core.tiling.TilingLayout
import com.wh01s17.launcher.core.tiling.dividers
import com.wh01s17.launcher.core.tiling.layout
import com.wh01s17.launcher.feature.workspaces.data.DeskRepository
import com.wh01s17.launcher.feature.workspaces.data.DeskStorage
import com.wh01s17.launcher.feature.workspaces.model.Desk
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
class WorkspacesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val personal = AppProfile(0, ProfileKind.Personal)
    private val work = AppProfile(10, ProfileKind.Work, available = false)

    private fun app(
        name: String,
        user: Long = 0,
    ) = AppEntry(AppKey("com.$name", "com.$name.Main", user), name)

    private val termux = app("termux")
    private val notes = app("notes")
    private val teams = app("teams", user = 10)

    private val catalog = MutableStateFlow(AppCatalog.of(listOf(personal, work), listOf(termux, notes, teams)))
    private val config = MutableStateFlow<LauncherConfig?>(LauncherConfig(1, BarConfig()))

    private class FakeActions : AppActions {
        var result: LaunchResult = LaunchResult.Launched
        val launched = mutableListOf<String>()

        override fun launch(
            app: AppEntry,
            origin: LaunchOrigin?,
        ) = result.also { launched += app.label }

        override fun openAppInfo(
            app: AppEntry,
            origin: LaunchOrigin?,
        ) = result

        override fun requestUninstall(app: AppEntry) = result

        override fun canReadShortcuts() = false

        override fun shortcuts(
            app: AppEntry,
            limit: Int,
        ) = emptyList<AppShortcut>()

        override fun launchShortcut(
            shortcut: AppShortcut,
            origin: LaunchOrigin?,
        ) = result
    }

    private object NoStorage : DeskStorage {
        override fun read(): String? = null

        override fun write(text: String) = Unit

        override fun keepBroken() = Unit
    }

    private val actions = FakeActions()

    /** Un teléfono en vertical: 1000 × 2000 px, 4 px entre tiles y 2 al borde, densidad 3. */
    private val metrics = CanvasMetrics(Rect(0, 0, 1000, 2000), Gaps(4, 2), density = 3f)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): Pair<WorkspacesViewModel, DeskRepository> {
        val desk = DeskRepository(NoStorage, backgroundScope, dispatcher)
        val vm = WorkspacesViewModel(desk, catalog, config, actions)
        backgroundScope.launch { vm.state.collect {} }
        runCurrent()
        vm.onIntent(WorkspacesIntent.CanvasChanged(metrics))
        return vm to desk
    }

    private fun WorkspacesViewModel.settle(scope: TestScope) = scope.runCurrent()

    @Test
    fun `picked apps become tiles of the current workspace with their app`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            assertTrue(vm.state.value.loaded)
            vm.addApp(termux.key)
            vm.addApp(teams.key)
            vm.settle(this)
            val state = vm.state.value
            assertEquals(listOf(TileId(1), TileId(2)), state.workspaces[0].tiles)
            assertEquals(TileInfo.App(termux.key, termux), state.tiles[TileId(1)])
            // El perfil de trabajo está en pausa: el tile se ve, atenuado.
            assertEquals(TileInfo.App(teams.key, teams, unavailable = true), state.tiles[TileId(2)])
        }

    @Test
    fun `tapping a tile focuses it and opens the app, except while editing`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            vm.addApp(termux.key)
            vm.addApp(notes.key)
            vm.settle(this)
            vm.onIntent(WorkspacesIntent.TapTile(TileId(1)))
            vm.settle(this)
            assertEquals(listOf("termux"), actions.launched)
            assertEquals(
                TileId(1),
                vm.state.value.workspaces[0]
                    .focused,
            )
            vm.onIntent(WorkspacesIntent.LongPressTile(TileId(2)))
            vm.settle(this)
            assertTrue(vm.state.value.editing)
            assertEquals(
                TileId(2),
                vm.state.value.workspaces[0]
                    .focused,
            )
            vm.onIntent(WorkspacesIntent.TapTile(TileId(1)))
            vm.settle(this)
            assertEquals(listOf("termux"), actions.launched)
            vm.onIntent(WorkspacesIntent.ExitEdit)
            vm.settle(this)
            assertFalse(vm.state.value.editing)
        }

    @Test
    fun `launch problems and uninstalled apps become a message`() =
        runTest(dispatcher) {
            val (vm, _) = viewModel()
            vm.addApp(termux.key)
            vm.addApp(AppKey("com.gone", "com.gone.Main", 0))
            vm.settle(this)
            vm.onIntent(WorkspacesIntent.TapTile(TileId(2)))
            vm.settle(this)
            assertEquals(WorkspaceMessage.NotInstalled, vm.state.value.message)
            vm.onIntent(WorkspacesIntent.MessageShown)
            actions.result = LaunchResult.ProfilePaused(resumeRequested = true)
            vm.onIntent(WorkspacesIntent.TapTile(TileId(1)))
            vm.settle(this)
            assertEquals(WorkspaceMessage.ProfileResumeRequested, vm.state.value.message)
            actions.result = LaunchResult.ProfilePaused(resumeRequested = false)
            vm.onIntent(WorkspacesIntent.TapTile(TileId(1)))
            vm.settle(this)
            assertEquals(WorkspaceMessage.ProfilePaused, vm.state.value.message)
            actions.result = LaunchResult.Failed("gone")
            vm.onIntent(WorkspacesIntent.TapTile(TileId(1)))
            vm.settle(this)
            assertEquals(WorkspaceMessage.LaunchFailed, vm.state.value.message)
            actions.result = LaunchResult.Launched
            vm.onIntent(WorkspacesIntent.TapTile(TileId(1)))
            vm.settle(this)
            assertNull(vm.state.value.message)
        }

    @Test
    fun `workspace commands move between workspaces and tiles`() =
        runTest(dispatcher) {
            val (vm, desk) = viewModel()
            vm.addApp(termux.key)
            vm.addApp(notes.key)
            vm.onCommand(WorkspaceCommand.MoveTileToWorkspace(4, follow = false))
            vm.settle(this)
            assertEquals(1, vm.state.value.current)
            assertEquals(setOf(1, 4), desk.state.value!!.occupied)
            vm.onCommand(WorkspaceCommand.CycleWorkspace(forward = true))
            vm.settle(this)
            assertEquals(4, vm.state.value.current)
            vm.onCommand(WorkspaceCommand.FocusWorkspace(1))
            vm.onIntent(WorkspacesIntent.PageSettled(2))
            vm.settle(this)
            assertEquals(2, vm.state.value.current)
            vm.onCommand(WorkspaceCommand.FocusWorkspace(4))
            vm.onCommand(WorkspaceCommand.MoveTileToWorkspace(1))
            vm.settle(this)
            assertEquals(1, vm.state.value.current)
            assertEquals(setOf(1), desk.state.value!!.occupied)
        }

    @Test
    fun `geometry commands use the canvas the screen reported`() =
        runTest(dispatcher) {
            val (vm, desk) = viewModel()
            vm.addApp(termux.key)
            vm.addApp(notes.key)
            vm.onCommand(WorkspaceCommand.MoveFocus(CommandDirection.Up))
            vm.settle(this)
            assertEquals(
                TileId(1),
                vm.state.value.workspaces[0]
                    .focused,
            )
            vm.onCommand(WorkspaceCommand.SwapTile(CommandDirection.Down))
            vm.settle(this)
            assertEquals(
                listOf(TileId(2), TileId(1)),
                vm.state.value.workspaces[0]
                    .tiles,
            )
            // 100 dp a densidad 3 = 300 px. El tile con el foco está abajo y no tiene borde inferior:
            // como `resizeactive`, dy > 0 baja su borde superior (300 de 1992 px).
            vm.onCommand(WorkspaceCommand.ResizeTile(dx = 0, dy = 100))
            vm.settle(this)
            val ws = desk.state.value!!.workspace(1)
            assertEquals(0.5f + 300f / 1992, (ws.root as Split).ratio, 1e-3f)
            // Alternar el split los pone uno al lado del otro: los dos empiezan arriba del todo.
            vm.onCommand(WorkspaceCommand.ToggleSplit)
            vm.settle(this)
            val tops = layout(desk.state.value!!.workspace(1), metrics.area, metrics.gaps).tiled.values.map { it.top }
            assertEquals(listOf(2, 2), tops)
        }

    /**
     * Regresión del Redmi (2026-11-04): con `current` en otro escritorio que el visible, mantener un
     * tile y pulsar «quitar» borraba el tile enfocado del escritorio equivocado.
     */
    @Test
    fun `holding a tile makes its workspace current, so closing removes that tile`() =
        runTest(dispatcher) {
            val (vm, desk) = viewModel()
            vm.onCommand(WorkspaceCommand.FocusWorkspace(3))
            vm.addApp(termux.key)
            vm.onCommand(WorkspaceCommand.FocusWorkspace(5))
            vm.addApp(notes.key)
            vm.onCommand(WorkspaceCommand.FocusWorkspace(3))
            vm.settle(this)
            vm.onIntent(WorkspacesIntent.LongPressTile(TileId(2)))
            vm.onCommand(WorkspaceCommand.CloseTile)
            vm.settle(this)
            val after = desk.state.value!!
            assertEquals(5, after.current)
            assertEquals("the visible tile goes", emptyList<TileId>(), after.workspace(5).tiles)
            assertEquals("the other workspace keeps its tile", listOf(TileId(1)), after.workspace(3).tiles)
        }

    @Test
    fun `toggles, layout, groups and closing act on the focused tile`() =
        runTest(dispatcher) {
            val (vm, desk) = viewModel()
            vm.addApp(termux.key)
            vm.addApp(notes.key)
            vm.onCommand(WorkspaceCommand.ToggleFloating)
            vm.settle(this)
            assertTrue(
                desk.state.value!!
                    .workspace(1)
                    .isFloating(TileId(2)),
            )
            vm.onCommand(WorkspaceCommand.ToggleFloating)
            vm.onCommand(WorkspaceCommand.ToggleFullscreen)
            vm.settle(this)
            assertEquals(
                TileId(2),
                desk.state.value!!
                    .workspace(1)
                    .fullscreen,
            )
            vm.onCommand(WorkspaceCommand.ToggleFullscreen)
            vm.onCommand(WorkspaceCommand.ToggleLayout)
            vm.settle(this)
            assertEquals(
                TilingLayout.Master,
                desk.state.value!!
                    .workspace(1)
                    .layout,
            )
            vm.onCommand(WorkspaceCommand.ToggleLayout)
            vm.onCommand(WorkspaceCommand.CycleGroup(forward = true))
            vm.onCommand(WorkspaceCommand.CloseTile)
            vm.settle(this)
            assertEquals(
                listOf(TileId(1)),
                desk.state.value!!
                    .workspace(1)
                    .tiles,
            )
            vm.onCommand(WorkspaceCommand.ToggleEditMode)
            vm.settle(this)
            assertTrue(vm.state.value.editing)
            vm.onCommand(WorkspaceCommand.ToggleEditMode)
            vm.onIntent(WorkspacesIntent.LongPressBackground)
            vm.settle(this)
            assertTrue(vm.state.value.editing)
        }

    @Test
    fun `edit gestures place, move, resize and float tiles`() =
        runTest(dispatcher) {
            val (vm, desk) = viewModel()
            vm.addApp(termux.key)
            vm.addApp(notes.key)
            vm.addApp(teams.key)
            // Soltar el 3 a la izquierda del 1 y deshacerlo: vuelve como estaba.
            val before = desk.state.value!!.workspace(1)
            vm.onIntent(WorkspacesIntent.MoveNextTo(TileId(3), TileId(1), Direction.Left))
            vm.settle(this)
            assertTrue(vm.state.value.canUndo)
            vm.onIntent(WorkspacesIntent.Undo)
            vm.settle(this)
            assertEquals(
                before.root,
                desk.state.value!!
                    .workspace(1)
                    .root,
            )
            // Soltar el 3 a la izquierda del 1: queda ahí, sin intercambiarse con él.
            vm.onIntent(WorkspacesIntent.MoveNextTo(TileId(3), TileId(1), Direction.Left))
            vm.settle(this)
            assertEquals(
                listOf(TileId(3), TileId(1), TileId(2)),
                desk.state.value!!
                    .workspace(1)
                    .tiles,
            )
            val divider = dividers(desk.state.value!!.workspace(1), metrics.area, metrics.gaps).first()
            vm.onIntent(WorkspacesIntent.DragDivider(divider, 600))
            vm.settle(this)
            // 3 y 1 comparten la mitad de arriba (3 a la izquierda); el divisor la deja en 600 px de alto.
            assertEquals(Rect(2, 2, 498, 598), layout(desk.state.value!!.workspace(1), metrics.area, metrics.gaps).tiled[TileId(3)])
            vm.onIntent(WorkspacesIntent.DropOnWorkspace(TileId(2), 5))
            vm.settle(this)
            assertEquals(
                listOf(TileId(2)),
                desk.state.value!!
                    .workspace(5)
                    .tiles,
            )
            assertEquals(1, vm.state.value.current)
            vm.onIntent(WorkspacesIntent.SelectTab(TileId(1)))
            vm.onCommand(WorkspaceCommand.ToggleFloating)
            val bounds = FloatingBounds(0.1f, 0.1f, 0.5f, 0.5f)
            vm.onIntent(WorkspacesIntent.MoveFloating(TileId(1), bounds))
            vm.onIntent(WorkspacesIntent.SelectTab(TileId(3)))
            vm.settle(this)
            assertEquals(
                bounds,
                desk.state.value!!
                    .workspace(1)
                    .floating
                    .single { it.id == TileId(1) }
                    .bounds,
            )
            assertEquals(
                TileId(3),
                desk.state.value!!
                    .workspace(1)
                    .focused,
            )
        }

    @Test
    fun `the config sets tile limits and layouts, and the animation`() =
        runTest(dispatcher) {
            val (vm, desk) = viewModel()
            config.value =
                LauncherConfig(1, BarConfig(), general = GeneralConfig(TilingLayout.Master, maxTilesPerWorkspace = 2, gapsIn = 6))
            vm.settle(this)
            assertEquals(
                TilingLayout.Master,
                desk.state.value!!
                    .workspace(3)
                    .layout,
            )
            assertEquals(
                2,
                desk.state.value!!
                    .workspace(3)
                    .maxTiles,
            )
            assertEquals(6, vm.state.value.general.gapsIn)
            // Una app nueva nunca se agrupa sola (el límite ya no se aplica); agrupar es explícito.
            vm.addApp(termux.key)
            vm.addApp(notes.key)
            vm.addApp(teams.key)
            vm.settle(this)
            assertEquals(
                3,
                desk.state.value!!
                    .workspace(1)
                    .slots,
            )
            vm.onCommand(WorkspaceCommand.ToggleGroup)
            vm.settle(this)
            assertEquals(
                2,
                desk.state.value!!
                    .workspace(1)
                    .slots,
            )
        }

    @Test
    fun `the scratchpad overlays the desk and takes the tile commands while shown`() =
        runTest(dispatcher) {
            val (vm, desk) = viewModel()
            vm.addApp(termux.key)
            vm.onCommand(WorkspaceCommand.ToggleScratchpad)
            vm.settle(this)
            assertTrue(vm.state.value.scratchpadVisible)
            // Con el scratchpad a la vista, lo nuevo va a él.
            vm.addApp(notes.key)
            vm.settle(this)
            assertEquals(
                listOf(TileId(2)),
                desk.state.value!!
                    .scratchpad.tiles,
            )
            // Llevarlo a un escritorio y ocultar el scratchpad.
            vm.onCommand(WorkspaceCommand.MoveTileToWorkspace(4, follow = false))
            vm.onIntent(WorkspacesIntent.HideScratchpad)
            vm.settle(this)
            assertEquals(
                listOf(TileId(2)),
                desk.state.value!!
                    .workspace(4)
                    .tiles,
            )
            assertFalse(vm.state.value.scratchpadVisible)
            // Y de vuelta al scratchpad sin seguirlo.
            vm.onIntent(WorkspacesIntent.SelectTab(TileId(1)))
            vm.onCommand(WorkspaceCommand.MoveTileToScratchpad)
            vm.settle(this)
            assertEquals(
                listOf(TileId(1)),
                desk.state.value!!
                    .scratchpad.tiles,
            )
            assertFalse(vm.state.value.scratchpadVisible)
        }

    @Test
    fun `without canvas metrics geometry commands wait`() =
        runTest(dispatcher) {
            val desk = DeskRepository(NoStorage, backgroundScope, dispatcher)
            val vm = WorkspacesViewModel(desk, catalog, config, actions)
            runCurrent()
            vm.addApp(termux.key)
            vm.addApp(notes.key)
            vm.onCommand(WorkspaceCommand.MoveFocus(CommandDirection.Up))
            runCurrent()
            assertEquals(
                TileId(2),
                desk.state.value!!
                    .workspace(1)
                    .focused,
            )
            assertEquals(Desk.FIRST, desk.state.value!!.current)
        }
}
