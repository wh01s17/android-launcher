package com.wh01s17.launcher.feature.workspaces.model

import com.wh01s17.launcher.core.apps.model.AppKey

/**
 * Qué muestra un tile (plan.md §5.3). Por ahora sólo apps; los widgets (Hito 7) y los tiles
 * nativos (reloj, pomodoro…) se añaden como nuevos casos, con su forma en [DeskCodec].
 */
sealed interface TileContent {
    /** Un lanzador vivo de la app [key]: al tocarlo se abre (plan.md §2.2). */
    data class App(
        val key: AppKey,
    ) : TileContent
}
