package com.wh01s17.launcher.feature.workspaces

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wh01s17.launcher.core.apps.actions.AppActions
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.actions.LaunchResult
import com.wh01s17.launcher.core.apps.model.AppCatalog
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.config.AnimationsConfig
import com.wh01s17.launcher.core.config.GeneralConfig
import com.wh01s17.launcher.core.config.LauncherConfig
import com.wh01s17.launcher.core.input.CommandDirection
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.tiling.Direction
import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.core.tiling.TilingLayout
import com.wh01s17.launcher.core.tiling.Workspace
import com.wh01s17.launcher.core.tiling.cycleGroup
import com.wh01s17.launcher.core.tiling.groupWith
import com.wh01s17.launcher.core.tiling.moveDivider
import com.wh01s17.launcher.core.tiling.moveFocus
import com.wh01s17.launcher.core.tiling.moveIntoGroup
import com.wh01s17.launcher.core.tiling.moveNextTo
import com.wh01s17.launcher.core.tiling.moveOutOfGroup
import com.wh01s17.launcher.core.tiling.resizeBy
import com.wh01s17.launcher.core.tiling.swapInDirection
import com.wh01s17.launcher.core.tiling.toggleFloating
import com.wh01s17.launcher.core.tiling.toggleFullscreen
import com.wh01s17.launcher.core.tiling.toggleGroup
import com.wh01s17.launcher.core.tiling.toggleSplit
import com.wh01s17.launcher.core.tiling.withFloatingBounds
import com.wh01s17.launcher.feature.workspaces.data.DeskRepository
import com.wh01s17.launcher.feature.workspaces.model.Desk
import com.wh01s17.launcher.feature.workspaces.model.TileContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Los escritorios en pantalla (plan.md §7 Hito 4): el workspace actual, sus tiles con su app y el
 * modo edición. Atiende los toques de la UI ([onIntent]) y los [WorkspaceCommand] del bus
 * ([onCommand]); los cambios van a [DeskRepository], que los guarda.
 */
class WorkspacesViewModel(
    private val desk: DeskRepository,
    catalog: StateFlow<AppCatalog>,
    config: Flow<LauncherConfig?>,
    private val actions: AppActions,
) : ViewModel() {
    private val editing = MutableStateFlow(false)
    private val message = MutableStateFlow<WorkspaceMessage?>(null)
    private var metrics: CanvasMetrics? = null

    private val settings =
        config
            .filterNotNull()
            .map { Settings(it.general, it.animations) }
            .distinctUntilChanged()

    val state: StateFlow<WorkspacesUiState> =
        combine(desk.state, catalog, settings.onStart { emit(Settings()) }, editing, message) { desk, catalog, settings, editing, message ->
            val base = WorkspacesUiState(general = settings.general, animation = settings.animations.workspaces)
            if (desk == null) {
                base
            } else {
                base.copy(
                    loaded = true,
                    current = desk.current,
                    workspaces = desk.workspaces,
                    tiles = tileInfos(desk, catalog),
                    appsLoaded = catalog.loaded,
                    editing = editing,
                    message = message,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WorkspacesUiState())

    init {
        viewModelScope.launch { settings.collect { desk.applyDefaults(it.general) } }
    }

    /** Añade un tile con [app] al escritorio actual (desde el selector de apps del modo edición). */
    fun addApp(app: AppKey) = desk.update { it.add(TileContent.App(app)) }

    /** Añade un widget ya enlazado (y configurado) al escritorio actual. */
    fun addWidget(appWidgetId: Int) = desk.update { it.add(TileContent.Widget(appWidgetId)) }

    /** Añade un widget propio ([TileContent.Native]) al escritorio actual. */
    fun addNative(id: String) = desk.update { it.add(TileContent.Native(id)) }

    fun onIntent(intent: WorkspacesIntent) {
        when (intent) {
            is WorkspacesIntent.PageSettled -> desk.update { it.focusWorkspace(intent.workspace) }
            is WorkspacesIntent.TapTile -> tap(intent.tile, intent.origin)
            is WorkspacesIntent.SelectTab -> focusTile(intent.tile)
            is WorkspacesIntent.LongPressTile -> {
                focusTile(intent.tile)
                editing.value = true
            }
            WorkspacesIntent.LongPressBackground -> editing.value = true
            WorkspacesIntent.ExitEdit -> editing.value = false
            is WorkspacesIntent.MoveNextTo -> current { it.moveNextTo(intent.dragged, intent.target, intent.side) }
            is WorkspacesIntent.GroupTiles -> current { it.groupWith(intent.dragged, intent.target) }
            is WorkspacesIntent.DropOnWorkspace -> desk.update { it.move(intent.tile, intent.workspace, follow = false) }
            is WorkspacesIntent.DragDivider -> geometry { ws, m -> ws.moveDivider(intent.divider, intent.positionPx, m.gaps) }
            is WorkspacesIntent.MoveFloating -> current { it.withFloatingBounds(intent.tile, intent.bounds) }
            is WorkspacesIntent.CanvasChanged -> metrics = intent.metrics
            WorkspacesIntent.MessageShown -> message.value = null
        }
    }

    fun onCommand(command: WorkspaceCommand) {
        when (command) {
            is WorkspaceCommand.FocusWorkspace -> desk.update { it.focusWorkspace(command.id) }
            is WorkspaceCommand.CycleWorkspace -> desk.update { it.cycleWorkspace(command.forward) }
            is WorkspaceCommand.MoveTileToWorkspace ->
                desk.update { d -> d.currentWorkspace.focused?.let { d.move(it, command.workspace, command.follow) } ?: d }
            WorkspaceCommand.CloseTile -> desk.update { d -> d.currentWorkspace.focused?.let(d::close) ?: d }
            WorkspaceCommand.ToggleEditMode -> editing.update { !it }
            else -> onTileCommand(command)
        }
    }

    /** Los comandos que sólo cambian el workspace actual (foco, tamaño, layout…). */
    private fun onTileCommand(command: WorkspaceCommand) {
        when (command) {
            is WorkspaceCommand.MoveFocus -> geometry { ws, m -> ws.moveFocus(command.direction.toTiling(), m.area, m.gaps) }
            is WorkspaceCommand.SwapTile -> geometry { ws, m -> ws.swapInDirection(command.direction.toTiling(), m.area, m.gaps) }
            is WorkspaceCommand.ResizeTile ->
                geometry {
                    ws,
                    m,
                    ->
                    ws.resizeBy((command.dx * m.density).roundToInt(), (command.dy * m.density).roundToInt(), m.area, m.gaps)
                }
            WorkspaceCommand.ToggleFloating -> current { it.toggleFloating() }
            WorkspaceCommand.ToggleSplit -> geometry { ws, m -> ws.toggleSplit(m.area, m.gaps) }
            WorkspaceCommand.ToggleFullscreen -> current { it.toggleFullscreen() }
            WorkspaceCommand.ToggleLayout -> current { it.withLayout(it.layout.other()) }
            is WorkspaceCommand.CycleGroup -> current { it.cycleGroup(command.forward) }
            WorkspaceCommand.ToggleGroup -> current { it.toggleGroup() }
            WorkspaceCommand.MoveOutOfGroup -> current { it.moveOutOfGroup() }
            is WorkspaceCommand.MoveIntoGroup -> geometry { ws, m -> ws.moveIntoGroup(command.direction.toTiling(), m.area, m.gaps) }
            else -> Unit // Los atiende `onCommand`.
        }
    }

    /** Fuera del modo edición, tocar un tile también abre su app (plan.md §2.2: lanzadores vivos). */
    private fun tap(
        tile: TileId,
        origin: LaunchOrigin?,
    ) {
        focusTile(tile)
        val info = state.value.tiles[tile]
        // Un widget recibe sus propios toques; aquí sólo llega el de las apps.
        if (editing.value || info !is TileInfo.App) return
        message.value = launch(info, origin)
    }

    /** Abre la app del tile; devuelve el aviso si no se pudo (o `null` si se abrió). */
    private fun launch(
        info: TileInfo.App,
        origin: LaunchOrigin?,
    ): WorkspaceMessage? {
        // Sin app: desinstalada si el índice ya se leyó; si no, aún no se sabe y no se avisa.
        val app = info.app ?: return WorkspaceMessage.NotInstalled.takeIf { state.value.appsLoaded }
        return when (val result = actions.launch(app, origin)) {
            LaunchResult.Launched -> null
            is LaunchResult.ProfilePaused ->
                if (result.resumeRequested) WorkspaceMessage.ProfileResumeRequested else WorkspaceMessage.ProfilePaused
            is LaunchResult.Failed -> WorkspaceMessage.LaunchFailed
        }
    }

    private fun current(transform: (Workspace) -> Workspace) = desk.update { it.updateCurrent(transform) }

    /**
     * Enfoca [tile] y hace actual **su** escritorio: lo que el usuario toca es lo que se ve, así que
     * las acciones siguientes («quitar», «añadir», atajos) van a ese escritorio aunque `current`
     * se hubiera quedado en otro.
     */
    private fun focusTile(tile: TileId) =
        desk.update { d -> d.workspaceOf(tile)?.let { w -> d.focusWorkspace(w).update(w) { it.focus(tile) } } ?: d }

    /** Operaciones que dependen de dónde cae cada tile: sin medidas del lienzo aún, no hacen nada. */
    private fun geometry(transform: (Workspace, CanvasMetrics) -> Workspace) {
        val metrics = metrics ?: return
        current { transform(it, metrics) }
    }

    private data class Settings(
        val general: GeneralConfig = GeneralConfig(),
        val animations: AnimationsConfig = AnimationsConfig(),
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        fun tileInfos(
            desk: Desk,
            catalog: AppCatalog,
        ): Map<TileId, TileInfo> {
            val apps = catalog.apps.associateBy { it.key }
            return desk.contents.mapValues { (_, content) ->
                when (content) {
                    is TileContent.App -> {
                        val app = apps[content.key]
                        val paused = catalog.profile(content.key.userSerial)?.available == false
                        TileInfo.App(content.key, app, unavailable = app != null && (paused || app.suspended))
                    }
                    is TileContent.Widget -> TileInfo.Widget(content.appWidgetId)
                    is TileContent.Native -> TileInfo.Native(content.id)
                }
            }
        }

        fun CommandDirection.toTiling(): Direction =
            when (this) {
                CommandDirection.Left -> Direction.Left
                CommandDirection.Right -> Direction.Right
                CommandDirection.Up -> Direction.Up
                CommandDirection.Down -> Direction.Down
            }

        fun TilingLayout.other(): TilingLayout = if (this == TilingLayout.Dwindle) TilingLayout.Master else TilingLayout.Dwindle
    }
}
