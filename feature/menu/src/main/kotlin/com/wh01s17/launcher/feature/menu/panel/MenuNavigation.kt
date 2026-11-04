package com.wh01s17.launcher.feature.menu.panel

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.wh01s17.launcher.feature.menu.search.MenuSearchState
import com.wh01s17.launcher.feature.menu.search.SearchIntent
import com.wh01s17.launcher.feature.menu.search.SearchTarget

internal class MenuCallbacks(
    val navigate: (MenuPage) -> Unit,
    val back: () -> Unit,
    val action: (MenuAction) -> Unit,
    val search: (SearchIntent) -> Unit,
)

/** El mismo recorrido para toque, IME y teclado. No ejecuta acciones de Android. */
internal class MenuNavigation(
    private val page: MenuPage,
    private val search: MenuSearchState,
    private val selected: Int,
    private val select: (Int) -> Unit,
    private val callbacks: MenuCallbacks,
) {
    private val entries = menuEntries(page)
    val searching = search.query.isNotEmpty() || page == MenuPage.Apps

    fun activate() {
        if (searching) {
            callbacks.search(SearchIntent.LaunchFirst)
        } else {
            val entry = entries.getOrNull(selected) ?: return
            entry.page?.let(callbacks.navigate)
            entry.action?.let(callbacks.action)
        }
    }

    fun back() {
        when {
            search.menu != null -> callbacks.search(SearchIntent.DismissMenu)
            search.query.isNotEmpty() -> callbacks.search(SearchIntent.ClearQuery)
            else -> callbacks.back()
        }
    }

    private fun contextMenu() {
        if (!searching) return
        val app = (search.results.getOrNull(search.selected)?.target as? SearchTarget.App)?.app
        if (app != null && !search.picking) callbacks.search(SearchIntent.ShowMenu(app))
    }

    private fun move(delta: Int) {
        if (searching) {
            callbacks.search(SearchIntent.MoveSelection(delta))
        } else if (entries.isNotEmpty()) {
            select(Math.floorMod(selected + delta, entries.size))
        }
    }

    fun onKey(event: KeyEvent): Boolean =
        when {
            event.key == Key.Escape -> released(event, ::back)
            event.key == Key.Menu || (event.key == Key.F10 && event.isShiftPressed) -> released(event, ::contextMenu)
            event.key in HORIZONTAL && search.query.isEmpty() -> released(event) { horizontal(event.key) }
            event.key in NAVIGATION -> released(event) { navigate(event) }
            else -> false
        }

    private fun horizontal(key: Key) {
        if (key == Key.DirectionLeft && page != MenuPage.Root) back()
        if (key == Key.DirectionRight && !searching) entries.getOrNull(selected)?.page?.let(callbacks.navigate)
    }

    private fun navigate(event: KeyEvent) {
        when (event.key) {
            Key.DirectionDown -> move(1)
            Key.DirectionUp -> move(-1)
            Key.Tab -> move(if (event.isShiftPressed) -1 else 1)
            else -> activate()
        }
    }

    private fun released(
        event: KeyEvent,
        action: () -> Unit,
    ): Boolean {
        if (event.type == KeyEventType.KeyUp) action()
        return true
    }

    companion object {
        private val HORIZONTAL = setOf(Key.DirectionLeft, Key.DirectionRight)
        private val NAVIGATION = setOf(Key.DirectionDown, Key.DirectionUp, Key.Tab, Key.Enter, Key.NumPadEnter)
    }
}
