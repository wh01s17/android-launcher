package com.wh01s17.launcher.feature.menu.drawer

import androidx.compose.runtime.Immutable
import com.wh01s17.launcher.core.apps.actions.AppShortcut
import com.wh01s17.launcher.core.apps.model.AppEntry

@Immutable
data class AppDrawerState(
    val open: Boolean = false,
    /**
     * Elegir una app para un tile (modo edición, plan.md §7 Hito 4) en vez de abrirla: tocarla la
     * entrega por [AppDrawerViewModel.picks] y no hay menú contextual.
     */
    val picking: Boolean = false,
    val query: String = "",
    /** `false` mientras el índice de apps aún no se ha leído. */
    val loaded: Boolean = false,
    val items: List<DrawerItem> = emptyList(),
    val menu: AppMenu? = null,
    val message: DrawerMessage? = null,
)

/** Una fila: la app, las letras que coinciden con la búsqueda y una marca opcional. */
@Immutable
class DrawerItem(
    val app: AppEntry,
    val matchPositions: IntArray,
    val badge: DrawerBadge?,
)

enum class DrawerBadge { ProfilePaused, Suspended, WorkProfile }

/** Menú contextual de una app (mantener pulsado). */
@Immutable
data class AppMenu(
    val app: AppEntry,
    val shortcuts: List<AppShortcut>,
    /** `false` si Android no deja leer atajos (no somos el launcher por defecto). */
    val shortcutsAvailable: Boolean,
    val loading: Boolean,
)

enum class DrawerMessage { ProfilePaused, ProfileResumeRequested, LaunchFailed }
