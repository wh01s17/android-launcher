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
    openShade: () -> Unit,
) {
    private val actions =
        MenuActions(
            activity,
            ::openApps,
            ::pick,
            onCommand = { command ->
                when (command) {
                    is WorkspaceCommand -> onWorkspace(command)
                    LauncherCommand.OpenNotificationShade -> openShade()
                    LauncherCommand.OpenMenu -> open()
                }
            },
            navigationActions = navigation,
        )

    fun open() {
        search.onIntent(SearchIntent.Open)
        menu.open()
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
