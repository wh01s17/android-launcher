package com.wh01s17.launcher.feature.workspaces.model

import com.wh01s17.launcher.core.apps.model.AppKey

/**
 * Qué muestra un tile (plan.md §5.3): una app o un widget del sistema. Los tiles nativos (reloj,
 * pomodoro…) se añaden como nuevos casos, con su forma en [DeskCodec].
 */
sealed interface TileContent {
    /** Un lanzador vivo de la app [key]: al tocarlo se abre (plan.md §2.2). */
    data class App(
        val key: AppKey,
    ) : TileContent

    /**
     * Un widget del sistema (plan.md §7 Hito 7), por su id en el `AppWidgetHost` del launcher. El id
     * se conserva entre versiones (plan.md §8.3); quitar el tile lo libera.
     */
    data class Widget(
        val appWidgetId: Int,
    ) : TileContent

    /**
     * Un widget propio de wh01s17 (reloj, pomodoro, metrónomo, uso del sistema, TIDAL…), por su
     * nombre (`"pomodoro"`). Lo pinta el [com.wh01s17.launcher.feature.workspaces.ui.WidgetRenderer].
     */
    data class Native(
        val id: String,
    ) : TileContent
}
