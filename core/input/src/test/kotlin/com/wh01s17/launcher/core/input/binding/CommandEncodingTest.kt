package com.wh01s17.launcher.core.input.binding

import com.wh01s17.launcher.core.input.CommandDirection
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandEncodingTest {
    private val samples: List<LauncherCommand> =
        listOf(
            LauncherCommand.OpenMenu,
            LauncherCommand.OpenAppsMenu,
            LauncherCommand.OpenSystemMenu,
            LauncherCommand.OpenNotificationShade,
            LauncherCommand.ShowShortcuts,
            LauncherCommand.OpenTerminal,
            LauncherCommand.AdjustVolume(-2),
            WorkspaceCommand.FocusWorkspace(10),
            WorkspaceCommand.CycleWorkspace(forward = true),
            WorkspaceCommand.CycleWorkspace(forward = false),
            WorkspaceCommand.ToggleLayout,
            WorkspaceCommand.MoveTileToWorkspace(4),
            WorkspaceCommand.MoveTileToWorkspace(4, follow = false),
            WorkspaceCommand.MoveFocus(CommandDirection.Up),
            WorkspaceCommand.SwapTile(CommandDirection.Left),
            WorkspaceCommand.ResizeTile(-20, 0),
            WorkspaceCommand.ResizeTile(5, 7),
            WorkspaceCommand.ToggleFloating,
            WorkspaceCommand.ToggleSplit,
            WorkspaceCommand.ToggleFullscreen,
            WorkspaceCommand.CloseTile,
            WorkspaceCommand.CycleGroup(forward = true),
            WorkspaceCommand.CycleGroup(forward = false),
            WorkspaceCommand.ToggleEditMode,
        )

    @Test
    fun `every command survives encoding and parsing`() {
        for (command in samples) {
            val encoded = command.encode()
            assertEquals(encoded.toString(), CommandParse.Valid(command), CommandCatalog.parse(encoded.name, encoded.args))
        }
        assertEquals("every catalog name is reachable", CommandCatalog.names.toSet(), samples.map { it.encode().name }.toSet())
    }

    @Test
    fun `default arguments are left out`() {
        assertEquals(EncodedCommand("tile.move", mapOf("id" to 4)), WorkspaceCommand.MoveTileToWorkspace(4).encode())
        assertEquals(EncodedCommand("tile.resize", mapOf("dx" to -20)), WorkspaceCommand.ResizeTile(-20, 0).encode())
    }
}
