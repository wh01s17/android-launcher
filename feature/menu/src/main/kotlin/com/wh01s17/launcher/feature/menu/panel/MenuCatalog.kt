package com.wh01s17.launcher.feature.menu.panel

import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.feature.menu.R

/** Navegación del panel; las acciones se resuelven en app, sin dependencias entre features. */
enum class MenuPage(
    val title: Int,
    val icon: WhGlyph,
) {
    Root(R.string.menu_title, WhIcons.Menu),
    Apps(R.string.menu_apps, WhIcons.Apps),
    Learn(R.string.menu_learn, WhIcons.Keyboard),
    Trigger(R.string.menu_trigger, WhIcons.Play),
    Style(R.string.menu_style, WhIcons.Theme),
    Setup(R.string.menu_setup, WhIcons.Settings),
    Install(R.string.menu_install, WhIcons.Plus),
    Remove(R.string.menu_remove, WhIcons.Trash),
    Update(R.string.menu_update, WhIcons.Refresh),
    About(R.string.menu_about, WhIcons.Info),
    System(R.string.menu_system, WhIcons.Power),
}

enum class MenuAction {
    Apps,
    AddApp,
    AddWidget,
    Edit,
    Layout,
    Floating,
    Split,
    Fullscreen,
    RemoveTile,
    NextWorkspace,
    PreviousWorkspace,
    Settings,
    SystemSettings,
    Accessibility,
    Wifi,
    Bluetooth,
    Audio,
    Display,
    Wallpaper,
    InstallApps,
    Notifications,
    Lock,
    PowerDialog,
    Shortcuts,
    GoWorkspace1,
    GoWorkspace2,
    GoWorkspace3,
    GoWorkspace4,
    GoWorkspace5,
    GoWorkspace6,
    GoWorkspace7,
    GoWorkspace8,
    GoWorkspace9,
    GoWorkspace10,
    MoveWorkspace1,
    MoveWorkspace2,
    MoveWorkspace3,
    MoveWorkspace4,
    MoveWorkspace5,
    MoveWorkspace6,
    MoveWorkspace7,
    MoveWorkspace8,
    MoveWorkspace9,
    MoveWorkspace10,
    FocusLeft,
    FocusRight,
    FocusUp,
    FocusDown,
    SwapLeft,
    SwapRight,
    SwapUp,
    SwapDown,
    GrowWidth,
    ShrinkWidth,
    GrowHeight,
    ShrinkHeight,
    NextGroup,
    PreviousGroup,
}

data class MenuEntry(
    val title: Int,
    val icon: WhGlyph,
    val page: MenuPage? = null,
    val action: MenuAction? = null,
)

internal fun menuEntries(page: MenuPage): List<MenuEntry> =
    when (page) {
        MenuPage.Root ->
            listOf(MenuEntry(R.string.menu_apps, WhIcons.Apps, action = MenuAction.Apps)) +
                MenuPage.entries.filter { it != MenuPage.Root && it != MenuPage.Apps }.map { MenuEntry(it.title, it.icon, page = it) }
        MenuPage.Trigger -> triggerEntries()
        MenuPage.Style ->
            listOf(
                MenuEntry(R.string.menu_display, WhIcons.Font, action = MenuAction.Display),
                MenuEntry(R.string.menu_wallpaper, WhIcons.Image, action = MenuAction.Wallpaper),
                MenuEntry(R.string.menu_layout, WhIcons.Layout, action = MenuAction.Layout),
            )
        MenuPage.Setup ->
            listOf(
                MenuEntry(R.string.menu_compat, WhIcons.Android, action = MenuAction.Settings),
                MenuEntry(R.string.menu_accessibility, WhIcons.Eye, action = MenuAction.Accessibility),
                MenuEntry(R.string.menu_system_settings, WhIcons.Settings, action = MenuAction.SystemSettings),
            )
        MenuPage.Install -> listOf(MenuEntry(R.string.menu_store, WhIcons.Plus, action = MenuAction.InstallApps))
        MenuPage.Remove -> listOf(MenuEntry(R.string.menu_choose_app, WhIcons.Apps, action = MenuAction.Apps))
        MenuPage.System -> systemEntries()
        MenuPage.Learn -> listOf(MenuEntry(R.string.menu_shortcuts, WhIcons.Keyboard, action = MenuAction.Shortcuts))
        MenuPage.Apps, MenuPage.Update, MenuPage.About -> emptyList()
    }

private fun triggerEntries() =
    listOf(
        MenuEntry(R.string.menu_add_app, WhIcons.Plus, action = MenuAction.AddApp),
        MenuEntry(R.string.menu_add_widget, WhIcons.Plus, action = MenuAction.AddWidget),
        MenuEntry(R.string.menu_edit, WhIcons.Pencil, action = MenuAction.Edit),
        MenuEntry(R.string.menu_layout, WhIcons.Layout, action = MenuAction.Layout),
        MenuEntry(R.string.menu_floating, WhIcons.Monitor, action = MenuAction.Floating),
        MenuEntry(R.string.menu_split, WhIcons.Layout, action = MenuAction.Split),
        MenuEntry(R.string.menu_fullscreen, WhIcons.Monitor, action = MenuAction.Fullscreen),
        MenuEntry(R.string.menu_remove_tile, WhIcons.Close, action = MenuAction.RemoveTile),
        MenuEntry(R.string.menu_next_workspace, WhIcons.ArrowRight, action = MenuAction.NextWorkspace),
        MenuEntry(R.string.menu_previous_workspace, WhIcons.ArrowLeft, action = MenuAction.PreviousWorkspace),
    )

private fun systemEntries() =
    listOf(
        MenuEntry(R.string.menu_lock, WhIcons.Lock, action = MenuAction.Lock),
        MenuEntry(R.string.menu_power_dialog, WhIcons.Power, action = MenuAction.PowerDialog),
        MenuEntry(R.string.menu_notifications, WhIcons.Bell, action = MenuAction.Notifications),
        MenuEntry(R.string.menu_wifi, WhIcons.Wifi, action = MenuAction.Wifi),
        MenuEntry(R.string.menu_bluetooth, WhIcons.Bluetooth, action = MenuAction.Bluetooth),
        MenuEntry(R.string.menu_audio, WhIcons.VolumeHigh, action = MenuAction.Audio),
        MenuEntry(R.string.menu_system_settings, WhIcons.Settings, action = MenuAction.SystemSettings),
    )

internal fun menuDescription(page: MenuPage): Int? =
    when (page) {
        MenuPage.Learn -> R.string.menu_learn_description
        MenuPage.Remove -> R.string.menu_remove_description
        MenuPage.Update -> R.string.menu_update_description
        MenuPage.About -> R.string.menu_about_description
        else -> null
    }

/** Acciones únicas con títulos localizados para el índice unificado. */
fun searchCommands(text: (Int) -> String): List<com.wh01s17.launcher.feature.menu.search.SearchTarget.Command> {
    val regular =
        MenuPage.entries
            .flatMap(::menuEntries)
            .filter { it.action != null }
            .distinctBy { it.action }
            .map { entry ->
                com.wh01s17.launcher.feature.menu.search.SearchTarget
                    .Command(requireNotNull(entry.action), text(entry.title), entry.icon)
            }
    val workspaces =
        (1..10).flatMap { id ->
            listOf(
                com.wh01s17.launcher.feature.menu.search.SearchTarget.Command(
                    MenuAction.valueOf("GoWorkspace$id"),
                    text(R.string.menu_go_workspace).format(id),
                    WhIcons.Monitor,
                ),
                com.wh01s17.launcher.feature.menu.search.SearchTarget.Command(
                    MenuAction.valueOf("MoveWorkspace$id"),
                    text(R.string.menu_move_workspace).format(id),
                    WhIcons.ArrowRight,
                ),
            )
        }
    val tiling =
        listOf(
            MenuAction.FocusLeft to R.string.menu_focus_left,
            MenuAction.FocusRight to R.string.menu_focus_right,
            MenuAction.FocusUp to R.string.menu_focus_up,
            MenuAction.FocusDown to R.string.menu_focus_down,
            MenuAction.SwapLeft to R.string.menu_swap_left,
            MenuAction.SwapRight to R.string.menu_swap_right,
            MenuAction.SwapUp to R.string.menu_swap_up,
            MenuAction.SwapDown to R.string.menu_swap_down,
            MenuAction.GrowWidth to R.string.menu_grow_width,
            MenuAction.ShrinkWidth to R.string.menu_shrink_width,
            MenuAction.GrowHeight to R.string.menu_grow_height,
            MenuAction.ShrinkHeight to R.string.menu_shrink_height,
            MenuAction.NextGroup to R.string.menu_next_group,
            MenuAction.PreviousGroup to R.string.menu_previous_group,
        ).map { (action, title) ->
            com.wh01s17.launcher.feature.menu.search.SearchTarget
                .Command(action, text(title), WhIcons.Layout)
        }
    return regular + workspaces + tiling
}
