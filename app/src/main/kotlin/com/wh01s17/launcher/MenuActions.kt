package com.wh01s17.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import com.wh01s17.launcher.core.input.CommandDirection
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.system.navigation.NotificationShade
import com.wh01s17.launcher.feature.menu.panel.MenuAction

/** Une el menú con Android y los otros features; el panel sólo declara acciones. */
internal class MenuActions(
    private val activity: Activity,
    private val openApps: () -> Unit,
    private val addApp: () -> Unit,
    private val onCommand: (LauncherCommand) -> Unit,
    private val navigationActions: NotificationShade,
) {
    fun execute(action: MenuAction): Boolean =
        when (action) {
            MenuAction.Apps -> {
                openApps()
                true
            }
            MenuAction.AddApp -> {
                addApp()
                true
            }
            MenuAction.Settings -> open(Intent(activity, SettingsActivity::class.java))
            MenuAction.Shortcuts -> open(SettingsActivity.shortcuts(activity))
            MenuAction.InstallApps -> open(activity.packageManager.getLaunchIntentForPackage("com.android.vending"))
            MenuAction.Lock -> requestAccessibility(navigationActions.lockScreen())
            MenuAction.PowerDialog -> requestAccessibility(navigationActions.powerDialog())
            MenuAction.Notifications -> requestAccessibility(navigationActions.open())
            else -> {
                val command = workspaceCommand(action)
                if (command != null) {
                    onCommand(command)
                    true
                } else {
                    openSystemScreen(action)
                }
            }
        }

    private fun requestAccessibility(done: Boolean): Boolean {
        if (done) return true
        AlertDialog
            .Builder(activity)
            .setMessage(com.wh01s17.launcher.feature.menu.R.string.menu_action_needs_accessibility)
            .setPositiveButton(com.wh01s17.launcher.feature.menu.R.string.menu_accessibility) { _, _ ->
                open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
        return false
    }

    private fun openSystemScreen(action: MenuAction): Boolean {
        val screen =
            when (action) {
                MenuAction.SystemSettings -> Settings.ACTION_SETTINGS
                MenuAction.Accessibility -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                MenuAction.Wifi -> Settings.ACTION_WIFI_SETTINGS
                MenuAction.Bluetooth -> Settings.ACTION_BLUETOOTH_SETTINGS
                MenuAction.Audio -> Settings.ACTION_SOUND_SETTINGS
                MenuAction.Display -> Settings.ACTION_DISPLAY_SETTINGS
                MenuAction.Wallpaper -> Intent.ACTION_SET_WALLPAPER
                else -> return false
            }
        return open(Intent(screen))
    }

    private fun open(intent: Intent?): Boolean {
        if (intent == null) {
            unavailable()
            return false
        }
        try {
            activity.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            unavailable()
        } catch (_: SecurityException) {
            unavailable()
        }
        return false
    }

    private fun unavailable() {
        Toast.makeText(activity, com.wh01s17.launcher.feature.menu.R.string.drawer_message_failed, Toast.LENGTH_LONG).show()
    }
}

private fun workspaceCommand(action: MenuAction): WorkspaceCommand? = WORKSPACE_ACTIONS[action] ?: workspaceDestination(action)

private val WORKSPACE_ACTIONS: Map<MenuAction, WorkspaceCommand> =
    mapOf(
        MenuAction.Edit to WorkspaceCommand.ToggleEditMode,
        MenuAction.Layout to WorkspaceCommand.ToggleLayout,
        MenuAction.Floating to WorkspaceCommand.ToggleFloating,
        MenuAction.Split to WorkspaceCommand.ToggleSplit,
        MenuAction.Fullscreen to WorkspaceCommand.ToggleFullscreen,
        MenuAction.RemoveTile to WorkspaceCommand.CloseTile,
        MenuAction.NextWorkspace to WorkspaceCommand.CycleWorkspace(forward = true),
        MenuAction.PreviousWorkspace to WorkspaceCommand.CycleWorkspace(forward = false),
        MenuAction.FocusLeft to WorkspaceCommand.MoveFocus(CommandDirection.Left),
        MenuAction.FocusRight to WorkspaceCommand.MoveFocus(CommandDirection.Right),
        MenuAction.FocusUp to WorkspaceCommand.MoveFocus(CommandDirection.Up),
        MenuAction.FocusDown to WorkspaceCommand.MoveFocus(CommandDirection.Down),
        MenuAction.SwapLeft to WorkspaceCommand.SwapTile(CommandDirection.Left),
        MenuAction.SwapRight to WorkspaceCommand.SwapTile(CommandDirection.Right),
        MenuAction.SwapUp to WorkspaceCommand.SwapTile(CommandDirection.Up),
        MenuAction.SwapDown to WorkspaceCommand.SwapTile(CommandDirection.Down),
        MenuAction.GrowWidth to WorkspaceCommand.ResizeTile(RESIZE_STEP, 0),
        MenuAction.ShrinkWidth to WorkspaceCommand.ResizeTile(-RESIZE_STEP, 0),
        MenuAction.GrowHeight to WorkspaceCommand.ResizeTile(0, RESIZE_STEP),
        MenuAction.ShrinkHeight to WorkspaceCommand.ResizeTile(0, -RESIZE_STEP),
        MenuAction.NextGroup to WorkspaceCommand.CycleGroup(true),
        MenuAction.PreviousGroup to WorkspaceCommand.CycleGroup(false),
    )

private fun workspaceDestination(action: MenuAction): WorkspaceCommand? {
    val name = action.name
    val go = name.removePrefix("GoWorkspace").toIntOrNull()
    val move = name.removePrefix("MoveWorkspace").toIntOrNull()
    return when {
        go != null -> WorkspaceCommand.FocusWorkspace(go)
        move != null -> WorkspaceCommand.MoveTileToWorkspace(move)
        else -> null
    }
}

/** Paso de los comandos de tamaño, en las unidades dp de WorkspaceCommand. */
private const val RESIZE_STEP = 20
