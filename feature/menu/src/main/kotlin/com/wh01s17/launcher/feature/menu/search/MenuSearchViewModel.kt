package com.wh01s17.launcher.feature.menu.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wh01s17.launcher.core.apps.AppRepository
import com.wh01s17.launcher.core.apps.actions.AppActions
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.actions.LaunchResult
import com.wh01s17.launcher.core.apps.model.AppCatalog
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.ProfileKind
import com.wh01s17.launcher.core.fuzzy.FuzzyIndex
import com.wh01s17.launcher.feature.menu.data.EmptyUsageRepository
import com.wh01s17.launcher.feature.menu.data.UsageRepository
import com.wh01s17.launcher.feature.menu.panel.MenuAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** Índice unificado, selección y acciones. La actividad resuelve los comandos sin entrar en el ViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class MenuSearchViewModel(
    repository: AppRepository,
    private val actions: AppActions,
    private val history: UsageRepository = EmptyUsageRepository,
    commands: List<SearchTarget.Command> = emptyList(),
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private data class Request(
        val open: Boolean = false,
        val picking: Boolean = false,
        val appsOnly: Boolean = false,
        val query: String = "",
        val inputEpoch: Int = 0,
    )

    private data class Matches(
        val loaded: Boolean,
        val query: String,
        val inputEpoch: Int,
        val results: List<SearchResult>,
    )

    private val request = MutableStateFlow(Request())
    private val selected = MutableStateFlow(0)
    private val menu = MutableStateFlow<AppMenu?>(null)
    private val message = MutableStateFlow<DrawerMessage?>(null)
    private val historyAvailable = MutableStateFlow(true)
    private val refresh = MutableStateFlow(0)
    private var pendingActivation: Pair<Int, String>? = null
    private val commandEvents = Channel<MenuAction>(Channel.BUFFERED)
    private val closeEvents = Channel<Unit>(Channel.BUFFERED)
    private val pickEvents = Channel<AppEntry>(Channel.BUFFERED)
    val commands = commandEvents.receiveAsFlow()
    val closed = closeEvents.receiveAsFlow()
    val picks = pickEvents.receiveAsFlow()

    private val searchable =
        combine(repository.catalog, refresh) { catalog, _ -> catalog }
            .mapLatest { catalog ->
                val apps = catalog.apps.map { SearchTarget.App(it) }
                val shortcuts =
                    withContext(ioDispatcher) {
                        if (actions.canReadShortcuts()) {
                            catalog.apps
                                .filter { !it.suspended && catalog.profile(it.key.userSerial)?.available != false }
                                .flatMap { app ->
                                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                                    actions.shortcuts(app).filter { it.enabled }.map { SearchTarget.Shortcut(it, app) }
                                }
                        } else {
                            emptyList()
                        }
                    }
                catalog to FuzzyIndex<SearchTarget>(apps + commands + shortcuts, { it.label }, { it.keywords })
            }.flowOn(workDispatcher)

    private val results =
        combine(searchable, request, history.usage) { (catalog, index), request, usage ->
            val matches =
                index
                    .search(request.query, { usage[it.id] }, nowMillis())
                    .filter { (!request.picking && (!request.appsOnly || request.query.isNotBlank())) || it.item is SearchTarget.App }
                    .map { result ->
                        val target = result.item
                        SearchResult(target, result.positions, (target as? SearchTarget.App)?.app?.let { catalog.badgeFor(it) })
                    }
            Matches(catalog.loaded, request.query, request.inputEpoch, matches)
        }.flowOn(workDispatcher)

    private val presentation =
        combine(selected, menu, message, historyAvailable) { selected, menu, message, available ->
            MenuSearchState(selected = selected, menu = menu, message = message, historyAvailable = available)
        }
    val state =
        combine(request, results, presentation) { request, matches, presentation ->
            val fresh = matches.query == request.query && matches.inputEpoch == request.inputEpoch
            val results = if (fresh) matches.results else emptyList()
            presentation.copy(
                open = request.open,
                picking = request.picking,
                query = request.query,
                inputEpoch = request.inputEpoch,
                loaded = matches.loaded && fresh,
                results = results,
                selected = presentation.selected.coerceIn(0, (results.size - 1).coerceAtLeast(0)),
            )
        }.onEach { ui ->
            if (pendingActivation == (ui.inputEpoch to ui.query) && ui.open && ui.loaded) {
                pendingActivation = null
                ui.results.getOrNull(ui.selected)?.let { activate(it.target) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MenuSearchState())

    fun onIntent(intent: SearchIntent) {
        if (handleNavigation(intent)) return
        when (intent) {
            is SearchIntent.Launch -> choose(intent.app, intent.origin)
            is SearchIntent.ShowMenu -> if (!request.value.picking) showMenu(intent.app)
            SearchIntent.DismissMenu -> menu.value = null
            is SearchIntent.OpenInfo -> handle(actions.openAppInfo(intent.app))
            is SearchIntent.Uninstall -> {
                menu.value = null
                handle(actions.requestUninstall(intent.app))
            }
            is SearchIntent.LaunchShortcut ->
                handle(
                    actions.launchShortcut(intent.shortcut, intent.origin),
                    "shortcut:${intent.shortcut.app}:${intent.shortcut.id}",
                )
            else -> Unit
        }
    }

    private fun handleNavigation(intent: SearchIntent): Boolean {
        when (intent) {
            SearchIntent.Open -> open()
            SearchIntent.OpenApps -> open(appsOnly = true)
            SearchIntent.OpenPicker -> open(picking = true)
            SearchIntent.Close -> close()
            SearchIntent.ClearQuery -> {
                pendingActivation = null
                request.update { it.copy(query = "", inputEpoch = it.inputEpoch + 1) }
                selected.value = 0
                message.value = null
            }
            is SearchIntent.QueryChanged -> {
                pendingActivation = null
                request.update { it.copy(query = intent.query) }
                selected.value = 0
                message.value = null
            }
            is SearchIntent.MoveSelection -> moveSelection(intent.delta)
            is SearchIntent.Activate -> activate(intent.target)
            SearchIntent.LaunchFirst -> activate()
            else -> return false
        }
        return true
    }

    private fun open(
        picking: Boolean = false,
        appsOnly: Boolean = false,
    ) {
        pendingActivation = null
        request.value = Request(open = true, picking = picking, appsOnly = appsOnly, inputEpoch = request.value.inputEpoch + 1)
        selected.value = 0
        menu.value = null
        message.value = null
        refresh.update { it + 1 }
    }

    private fun moveSelection(delta: Int) {
        val size = state.value.results.size
        if (size != 0) selected.value = Math.floorMod(state.value.selected + delta, size)
    }

    private fun activate(target: SearchTarget? = null) {
        if (target == null) {
            val ui = state.value
            if (ui.loaded && ui.query == request.value.query && ui.inputEpoch == request.value.inputEpoch) {
                ui.results.getOrNull(ui.selected)?.let { activate(it.target) }
            } else {
                pendingActivation = request.value.inputEpoch to request.value.query
            }
            return
        }
        if (request.value.picking && target !is SearchTarget.App) return
        when (target) {
            is SearchTarget.App -> choose(target.app, null)
            is SearchTarget.Shortcut -> handle(actions.launchShortcut(target.shortcut), target.id)
            is SearchTarget.Command -> commandEvents.trySend(target.action)
        }
    }

    private fun choose(
        app: AppEntry,
        origin: LaunchOrigin?,
    ) {
        if (request.value.picking) {
            pickEvents.trySend(app)
            close()
        } else {
            handle(actions.launch(app, origin), "app:${app.key}")
        }
    }

    fun commandFinished(
        action: MenuAction,
        succeeded: Boolean,
    ) {
        if (succeeded) handle(LaunchResult.Launched, "command:${action.name}") else handle(LaunchResult.Failed("unavailable"))
    }

    private fun close() {
        pendingActivation = null
        request.value = Request(inputEpoch = request.value.inputEpoch + 1)
        menu.value = null
        message.value = null
        selected.value = 0
        closeEvents.trySend(Unit)
    }

    private fun handle(
        result: LaunchResult,
        usageId: String? = null,
    ) {
        if (result == LaunchResult.Launched) {
            if (usageId != null) record(usageId)
            close()
        } else {
            message.value =
                when (result) {
                    is LaunchResult.ProfilePaused ->
                        if (result.resumeRequested) DrawerMessage.ProfileResumeRequested else DrawerMessage.ProfilePaused
                    else -> DrawerMessage.LaunchFailed
                }
            menu.value = null
        }
    }

    /** El ámbito de DataStore vive en el proceso; cerrar el panel no cancela la escritura. */
    private fun record(id: String) {
        viewModelScope.launch {
            try {
                history.record(id)
                historyAvailable.value = true
            } catch (_: IOException) {
                historyAvailable.value = false
            }
        }
    }

    private fun showMenu(app: AppEntry) {
        menu.value = AppMenu(app, emptyList(), shortcutsAvailable = true, loading = true)
        viewModelScope.launch {
            val (available, shortcuts) =
                withContext(workDispatcher) {
                    val available = actions.canReadShortcuts()
                    available to if (available) actions.shortcuts(app) else emptyList()
                }
            menu.update { current -> if (current?.app?.key == app.key) AppMenu(app, shortcuts, available, loading = false) else current }
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        private fun AppCatalog.badgeFor(app: AppEntry): DrawerBadge? {
            val profile = profile(app.key.userSerial)
            return when {
                profile != null && !profile.available -> DrawerBadge.ProfilePaused
                app.suspended -> DrawerBadge.Suspended
                profile != null && profile.kind != ProfileKind.Personal -> DrawerBadge.WorkProfile
                else -> null
            }
        }
    }
}
