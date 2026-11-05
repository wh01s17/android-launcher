package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.tiling.TilingLayout
import com.wh01s17.launcher.core.tiling.Workspace
import com.wh01s17.launcher.feature.workspaces.R

/**
 * Acciones del modo edición sobre el tile con el foco (plan.md §7 Hito 4): añadir, alternar split,
 * flotante y pantalla completa, cambiar el layout del escritorio y quitar. Son los mismos
 * [WorkspaceCommand] que los atajos de teclado (Hito 6). Aquí también están los ajustes, porque
 * mantener pulsado el fondo ya no los abre.
 */
@Composable
internal fun EditToolbar(
    workspace: Workspace,
    onCommand: (WorkspaceCommand) -> Unit,
    onAddTile: () -> Unit,
    onOpenSettings: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onAddWidget: () -> Unit = {},
    canUndo: Boolean = false,
    onUndo: () -> Unit = {},
) {
    val spacing = WhTheme.spacing
    val focused = workspace.focused
    val tiled = focused != null && !workspace.isFloating(focused)
    WhPanel(
        modifier.fillMaxWidth().padding(spacing.controlGap),
        style = WhTheme.surfaces.launcher.surface,
        contentPadding = spacing.controlGap,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.controlGap),
            verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
        ) {
            Action(stringResource(R.string.edit_undo), enabled = canUndo, onClick = onUndo)
            Action(stringResource(R.string.edit_add), onClick = onAddTile)
            Action(stringResource(R.string.edit_add_widget), onClick = onAddWidget)
            Action(
                stringResource(R.string.edit_split),
                enabled = tiled && workspace.layout == TilingLayout.Dwindle && workspace.slots > 1,
            ) { onCommand(WorkspaceCommand.ToggleSplit) }
            Action(stringResource(R.string.edit_float), enabled = focused != null, selected = focused != null && !tiled) {
                onCommand(WorkspaceCommand.ToggleFloating)
            }
            Action(stringResource(R.string.edit_fullscreen), enabled = focused != null, selected = workspace.fullscreen != null) {
                onCommand(WorkspaceCommand.ToggleFullscreen)
            }
            // El nombre del layout actual, como el `Super + L` del escritorio: tocar cambia al otro.
            Action(workspace.layout.configName) { onCommand(WorkspaceCommand.ToggleLayout) }
            // Agrupar: con el tile tocado antes (toca uno, luego otro, y agrupar). En un grupo, lo deshace.
            val inGroup = tiled && workspace.root?.leaves()?.any { focused in it.tiles && it.isGroup } == true
            Action(
                stringResource(if (inGroup) R.string.edit_ungroup else R.string.edit_group),
                enabled =
                    tiled && workspace.slots > 1 || inGroup,
            ) {
                onCommand(WorkspaceCommand.ToggleGroup)
            }
            if (inGroup) Action(stringResource(R.string.edit_group_out)) { onCommand(WorkspaceCommand.MoveOutOfGroup) }
            Action(stringResource(R.string.edit_close), enabled = focused != null) { onCommand(WorkspaceCommand.CloseTile) }
            Action(stringResource(R.string.edit_settings), onClick = onOpenSettings)
            Action(stringResource(R.string.edit_done), selected = true, onClick = onDone)
        }
        WhText(stringResource(R.string.edit_hint), style = WhTheme.typography.caption, color = WhTheme.colors.darkForeground)
    }
}

@Composable
private fun Action(
    text: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    WhButton(onClick = onClick, enabled = enabled, selected = selected) { WhText(text) }
}
