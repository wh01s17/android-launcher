package com.wh01s17.launcher.feature.menu.search

import com.wh01s17.launcher.core.apps.actions.AppShortcut
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.model.AppEntry

sealed interface SearchIntent {
    data object Open : SearchIntent

    data object OpenApps : SearchIntent

    data object OpenPicker : SearchIntent

    data object Close : SearchIntent

    data object ClearQuery : SearchIntent

    data class QueryChanged(
        val query: String,
    ) : SearchIntent

    data class Launch(
        val app: AppEntry,
        val origin: LaunchOrigin? = null,
    ) : SearchIntent

    data class Activate(
        val target: SearchTarget,
    ) : SearchIntent

    data object LaunchFirst : SearchIntent

    data class MoveSelection(
        val delta: Int,
    ) : SearchIntent

    data class ShowMenu(
        val app: AppEntry,
    ) : SearchIntent

    data object DismissMenu : SearchIntent

    data class OpenInfo(
        val app: AppEntry,
    ) : SearchIntent

    data class Uninstall(
        val app: AppEntry,
    ) : SearchIntent

    data class LaunchShortcut(
        val shortcut: AppShortcut,
        val origin: LaunchOrigin? = null,
    ) : SearchIntent
}
