package com.wh01s17.launcher.feature.menu.search

import com.wh01s17.launcher.core.apps.actions.AppShortcut
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.feature.menu.panel.MenuAction

sealed interface SearchTarget {
    val id: String
    val label: String
    val keywords: List<String>

    data class App(
        val app: AppEntry,
    ) : SearchTarget {
        override val id = "app:${app.key}"
        override val label = app.label
        override val keywords = listOf(app.packageName)
    }

    data class Command(
        val action: MenuAction,
        override val label: String,
        val icon: WhGlyph,
    ) : SearchTarget {
        override val id = "command:${action.name}"
        override val keywords = listOf(action.name)
    }

    data class Shortcut(
        val shortcut: AppShortcut,
        val app: AppEntry,
    ) : SearchTarget {
        override val id = "shortcut:${app.key}:${shortcut.id}"
        override val label = "${app.label}: ${shortcut.shortLabel}"
        override val keywords = listOfNotNull(app.packageName, shortcut.longLabel)
    }
}

class SearchResult(
    val target: SearchTarget,
    val positions: IntArray,
    val badge: DrawerBadge? = null,
)

data class MenuSearchState(
    val open: Boolean = false,
    val picking: Boolean = false,
    val query: String = "",
    val inputEpoch: Int = 0,
    val loaded: Boolean = false,
    val results: List<SearchResult> = emptyList(),
    val selected: Int = 0,
    val menu: AppMenu? = null,
    val message: DrawerMessage? = null,
    val historyAvailable: Boolean = true,
)
