package com.wh01s17.launcher.core.apps.actions

import android.graphics.Rect
import android.view.View
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey

/**
 * Desde dónde se abre algo: la vista que lo dibuja y el rectángulo del icono en coordenadas de esa
 * vista. Con él, la ventana nueva crece desde el icono (`ActivityOptions.makeScaleUpAnimation`).
 */
class LaunchOrigin(
    val view: View,
    val bounds: Rect,
)

/** Resultado de intentar abrir una app o un atajo. */
sealed interface LaunchResult {
    data object Launched : LaunchResult

    /**
     * El perfil (de trabajo) está en pausa. Si [resumeRequested], ya se pidió reanudarlo: el
     * sistema pregunta al usuario y, al volver, la app se puede abrir.
     */
    data class ProfilePaused(
        val resumeRequested: Boolean,
    ) : LaunchResult

    /** La app o el atajo ya no existe, está desactivado o el sistema lo impidió. */
    data class Failed(
        val reason: String,
    ) : LaunchResult
}

/** Atajo de app (`ShortcutInfo`), sin tipos de Android. */
data class AppShortcut(
    val id: String,
    val app: AppKey,
    val shortLabel: String,
    val longLabel: String?,
    val enabled: Boolean,
    /** Por qué está desactivado, si lo explica la app. */
    val disabledMessage: String?,
    /** Orden de la app: menor primero. Los del manifiesto van antes que los dinámicos. */
    val rank: Int,
    val fromManifest: Boolean,
)

/** Acciones sobre apps (plan.md §5.8): abrir, info, desinstalar y atajos. */
interface AppActions {
    fun launch(
        app: AppEntry,
        origin: LaunchOrigin? = null,
    ): LaunchResult

    fun openAppInfo(
        app: AppEntry,
        origin: LaunchOrigin? = null,
    ): LaunchResult

    /** Abre el diálogo de desinstalación del sistema; nunca desinstala sin confirmar. */
    fun requestUninstall(app: AppEntry): LaunchResult

    /** `true` sólo si el launcher es el HOME por defecto: Android no da atajos a otros. */
    fun canReadShortcuts(): Boolean

    /** Atajos para el menú de la app, ordenados y como mucho [limit]. Vacío si no se pueden leer. */
    fun shortcuts(
        app: AppEntry,
        limit: Int = MAX_MENU_SHORTCUTS,
    ): List<AppShortcut>

    fun launchShortcut(
        shortcut: AppShortcut,
        origin: LaunchOrigin? = null,
    ): LaunchResult

    companion object {
        /** Como Launcher3: cuatro atajos caben en el menú sin desplazarse. */
        const val MAX_MENU_SHORTCUTS = 4
    }
}

/**
 * Orden del menú de atajos: primero los del manifiesto y luego los dinámicos, cada grupo por su
 * `rank`; los desactivados al final. Como Launcher3, que también los mezcla así.
 */
fun List<AppShortcut>.forMenu(limit: Int): List<AppShortcut> =
    sortedWith(compareBy<AppShortcut> { !it.enabled }.thenBy { !it.fromManifest }.thenBy { it.rank }.thenBy { it.id })
        .distinctBy { it.id }
        .take(limit.coerceAtLeast(0))
