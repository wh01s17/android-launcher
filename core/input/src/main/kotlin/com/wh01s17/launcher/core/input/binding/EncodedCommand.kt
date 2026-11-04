package com.wh01s17.launcher.core.input.binding

import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand

/** Un comando como se escribe en `bindings`: nombre y argumentos (`Int`, `Boolean` o `String`). */
data class EncodedCommand(
    val name: String,
    val args: Map<String, Any> = emptyMap(),
)

/**
 * La inversa de [CommandCatalog.parse], para la tabla de `Super + K` y para escribir `bindings` desde
 * el editor. El `when` es exhaustivo: un comando nuevo no compila hasta tener nombre.
 */
@Suppress("CyclomaticComplexMethod") // Una rama por comando; dividirlo sólo esconde la lista.
fun LauncherCommand.encode(): EncodedCommand =
    when (this) {
        LauncherCommand.OpenMenu -> EncodedCommand("menu.open")
        LauncherCommand.OpenAppsMenu -> EncodedCommand("menu.apps")
        LauncherCommand.OpenSystemMenu -> EncodedCommand("menu.system")
        LauncherCommand.OpenNotificationShade -> EncodedCommand("notifications.open")
        LauncherCommand.ShowShortcuts -> EncodedCommand("shortcuts.show")
        LauncherCommand.OpenTerminal -> EncodedCommand("terminal.open")
        is LauncherCommand.AdjustVolume -> EncodedCommand("audio.volume", mapOf("step" to steps))
        is WorkspaceCommand.FocusWorkspace -> EncodedCommand("workspace.focus", mapOf("id" to id))
        is WorkspaceCommand.CycleWorkspace -> EncodedCommand(if (forward) "workspace.next" else "workspace.previous")
        WorkspaceCommand.ToggleLayout -> EncodedCommand("workspace.layout")
        is WorkspaceCommand.MoveTileToWorkspace ->
            EncodedCommand("tile.move", if (follow) mapOf("id" to workspace) else mapOf("id" to workspace, "follow" to false))
        is WorkspaceCommand.MoveFocus -> EncodedCommand("tile.focus", mapOf("direction" to direction.name.lowercase()))
        is WorkspaceCommand.SwapTile -> EncodedCommand("tile.swap", mapOf("direction" to direction.name.lowercase()))
        is WorkspaceCommand.ResizeTile ->
            EncodedCommand(
                "tile.resize",
                buildMap {
                    if (dx != 0) put("dx", dx)
                    if (dy != 0) put("dy", dy)
                },
            )
        WorkspaceCommand.ToggleFloating -> EncodedCommand("tile.float")
        WorkspaceCommand.ToggleSplit -> EncodedCommand("tile.split")
        WorkspaceCommand.ToggleFullscreen -> EncodedCommand("tile.fullscreen")
        WorkspaceCommand.CloseTile -> EncodedCommand("tile.close")
        is WorkspaceCommand.CycleGroup -> EncodedCommand(if (forward) "group.next" else "group.previous")
        WorkspaceCommand.ToggleEditMode -> EncodedCommand("edit.toggle")
    }
