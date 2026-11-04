package com.wh01s17.launcher.feature.menu.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wh01s17.launcher.core.apps.AppRepository
import com.wh01s17.launcher.core.apps.actions.AppActions
import com.wh01s17.launcher.core.apps.actions.AppShortcut
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.actions.LaunchResult
import com.wh01s17.launcher.core.apps.model.AppCatalog
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.ProfileKind
import com.wh01s17.launcher.core.fuzzy.FuzzyIndex
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lo que el usuario puede hacer en el drawer (UDF, plan.md §5.1). */
sealed interface DrawerIntent {
    data object Open : DrawerIntent

    /** Abrir para elegir una app (añadir un tile) en vez de lanzarla. */
    data object OpenPicker : DrawerIntent

    data object Close : DrawerIntent

    data class QueryChanged(
        val query: String,
    ) : DrawerIntent

    data class Launch(
        val app: AppEntry,
        val origin: LaunchOrigin? = null,
    ) : DrawerIntent

    /** Enter en la búsqueda: abre el primer resultado. */
    data object LaunchFirst : DrawerIntent

    data class ShowMenu(
        val app: AppEntry,
    ) : DrawerIntent

    data object DismissMenu : DrawerIntent

    data class OpenInfo(
        val app: AppEntry,
    ) : DrawerIntent

    data class Uninstall(
        val app: AppEntry,
    ) : DrawerIntent

    data class LaunchShortcut(
        val shortcut: AppShortcut,
        val origin: LaunchOrigin? = null,
    ) : DrawerIntent
}

/**
 * Estado del drawer provisional (plan.md §7, Hito 2): búsqueda difusa sobre el índice de apps y
 * menú contextual (info, desinstalar, atajos). Se sustituye por el menú wh01s17 en el Hito 5.
 *
 * El índice difuso se rehace sólo cuando cambia el catálogo; cada tecla sólo busca. Las dos cosas
 * corren en [workDispatcher], fuera del hilo principal.
 */
class AppDrawerViewModel(
    repository: AppRepository,
    private val actions: AppActions,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val open = MutableStateFlow(false)
    private val picking = MutableStateFlow(false)
    private val pickedApps = MutableSharedFlow<AppEntry>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Las apps elegidas en modo [DrawerIntent.OpenPicker]; quien abrió el selector decide qué hacer. */
    val picks: SharedFlow<AppEntry> = pickedApps.asSharedFlow()
    private val query = MutableStateFlow("")
    private val menu = MutableStateFlow<AppMenu?>(null)
    private val message = MutableStateFlow<DrawerMessage?>(null)

    private val searchable =
        repository.catalog
            .map { catalog -> Searchable(catalog, FuzzyIndex(catalog.apps, { it.label }, { listOf(it.packageName) })) }
            .flowOn(workDispatcher)

    private val results =
        combine(searchable, query) { (catalog, index), q ->
            Results(catalog.loaded, index.search(q).map { DrawerItem(it.item, it.positions, catalog.badgeFor(it.item)) })
        }.flowOn(workDispatcher)

    private val mode = combine(open, picking) { open, picking -> open to (open && picking) }

    val state: StateFlow<AppDrawerState> =
        combine(mode, query, results, menu, message) { (open, picking), query, results, menu, message ->
            AppDrawerState(open, picking, query, results.loaded, results.items, menu, message)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppDrawerState())

    fun onIntent(intent: DrawerIntent) {
        when (intent) {
            DrawerIntent.Open -> {
                picking.value = false
                open.value = true
            }
            DrawerIntent.OpenPicker -> {
                picking.value = true
                open.value = true
            }
            DrawerIntent.Close -> close()
            is DrawerIntent.QueryChanged -> {
                query.value = intent.query
                message.value = null
            }
            is DrawerIntent.Launch -> choose(intent.app, intent.origin)
            DrawerIntent.LaunchFirst ->
                state.value.items
                    .firstOrNull()
                    ?.let { choose(it.app, null) }
            is DrawerIntent.ShowMenu -> if (!picking.value) showMenu(intent.app)
            DrawerIntent.DismissMenu -> menu.value = null
            is DrawerIntent.OpenInfo -> handle(actions.openAppInfo(intent.app))
            is DrawerIntent.Uninstall -> {
                menu.value = null
                handleFailure(actions.requestUninstall(intent.app))
            }
            is DrawerIntent.LaunchShortcut -> handle(actions.launchShortcut(intent.shortcut, intent.origin))
        }
    }

    /** Abrir la app o, eligiendo para un tile, entregarla y cerrar. */
    private fun choose(
        app: AppEntry,
        origin: LaunchOrigin?,
    ) {
        if (picking.value) {
            pickedApps.tryEmit(app)
            close()
        } else {
            handle(actions.launch(app, origin))
        }
    }

    private fun close() {
        open.value = false
        picking.value = false
        query.value = ""
        menu.value = null
        message.value = null
    }

    /** Si se abrió algo, el drawer se cierra; si no, se explica por qué. */
    private fun handle(result: LaunchResult) {
        if (result == LaunchResult.Launched) close() else handleFailure(result)
    }

    private fun handleFailure(result: LaunchResult) {
        message.value =
            when (result) {
                LaunchResult.Launched -> null
                is LaunchResult.ProfilePaused ->
                    if (result.resumeRequested) DrawerMessage.ProfileResumeRequested else DrawerMessage.ProfilePaused
                is LaunchResult.Failed -> DrawerMessage.LaunchFailed
            }
        if (result != LaunchResult.Launched) menu.value = null
    }

    private fun showMenu(app: AppEntry) {
        menu.value = AppMenu(app, shortcuts = emptyList(), shortcutsAvailable = true, loading = true)
        viewModelScope.launch {
            // Leer atajos es una llamada al sistema: fuera del hilo principal.
            val (available, shortcuts) =
                withContext(workDispatcher) {
                    actions.canReadShortcuts().let {
                        it to
                            if (it) actions.shortcuts(app) else emptyList()
                    }
                }
            menu.update { current -> if (current?.app?.key == app.key) AppMenu(app, shortcuts, available, loading = false) else current }
        }
    }

    private data class Searchable(
        val catalog: AppCatalog,
        val index: FuzzyIndex<AppEntry>,
    )

    private class Results(
        val loaded: Boolean,
        val items: List<DrawerItem>,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        fun AppCatalog.badgeFor(app: AppEntry): DrawerBadge? {
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
