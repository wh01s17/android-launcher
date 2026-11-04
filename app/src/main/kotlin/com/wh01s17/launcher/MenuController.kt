package com.wh01s17.launcher

import android.app.Activity
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.system.navigation.NotificationShade
import com.wh01s17.launcher.feature.menu.panel.MenuAction
import com.wh01s17.launcher.feature.menu.panel.MenuPage
import com.wh01s17.launcher.feature.menu.panel.MenuViewModel
import com.wh01s17.launcher.feature.menu.search.MenuSearchViewModel
import com.wh01s17.launcher.feature.menu.search.SearchIntent

/** Coordinación de features y acciones Android, con la misma vida que la actividad. */
internal class MenuController(
    activity: Activity,
    private val menu: MenuViewModel,
    private val search: MenuSearchViewModel,
    navigation: NotificationShade,
    onWorkspace: (WorkspaceCommand) -> Unit,
    private val onOther: (LauncherCommand) -> Unit,
) {
    private val actions =
        MenuActions(
            activity,
            ::openApps,
            ::pick,
            onCommand = { command ->
                when (command) {
                    is WorkspaceCommand -> onWorkspace(command)
                    else -> onOther(command)
                }
            },
            navigationActions = navigation,
        )

    fun open() {
        search.onIntent(SearchIntent.Open)
        menu.open()
    }

    /**
     * `wh01s17-menu toggle [page]` del escritorio: abre el menú en [page], o lo cierra si ya está
     * ahí. Así `Super + Space` (o `Super + Escape`) dos veces abre y cierra.
     */
    fun toggle(page: MenuPage) {
        if (menu.page.value == page) {
            close()
            return
        }
        open()
        when (page) {
            MenuPage.Root -> Unit
            MenuPage.Apps -> openApps()
            else -> menu.navigate(page)
        }
    }

    private fun openApps() {
        search.onIntent(SearchIntent.OpenApps)
        menu.navigate(MenuPage.Apps)
    }

    fun pick() {
        search.onIntent(SearchIntent.OpenPicker)
        menu.open()
        menu.navigate(MenuPage.Apps)
    }

    fun close() {
        menu.close()
        search.onIntent(SearchIntent.Close)
    }

    fun back() {
        val state = search.state.value
        when {
            state.menu != null -> search.onIntent(SearchIntent.DismissMenu)
            state.query.isNotEmpty() -> search.onIntent(SearchIntent.ClearQuery)
            menu.page.value == MenuPage.Root -> close()
            else -> open()
        }
    }

    fun execute(action: MenuAction) {
        val done = actions.execute(action)
        if (action == MenuAction.Apps || action == MenuAction.AddApp) return
        search.commandFinished(action, done)
        if (done) menu.close()
    }
}
