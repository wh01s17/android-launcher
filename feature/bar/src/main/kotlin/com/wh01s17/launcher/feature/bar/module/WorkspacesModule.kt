package com.wh01s17.launcher.feature.bar.module

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.feature.bar.R
import com.wh01s17.launcher.feature.bar.WorkspaceBarState

/** Opacidad de un escritorio vacío (`Workspaces.qml`). */
private const val EMPTY_ALPHA = 0.35f

/**
 * `core.workspaces`: sólo los números, como en el shell. El escritorio actual en verde con un
 * subrayado corto, los que tienen apps en el color del texto y los vacíos apagados. Se ven siempre
 * los `persistent` primeros (opción del módulo; 5 por defecto, como el shell) más los ocupados y el
 * activo; el resto de vacíos se oculta, como en Hyprland. Tocar uno emite `FocusWorkspace`.
 */
internal class WorkspacesModule : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val state = context.state.workspaces
        val persistent = context.settings.int(PERSISTENT)?.coerceIn(0, WorkspaceBarState.LAST) ?: state.persistent
        val workspaces = state.copy(persistent = persistent)
        Row {
            for (id in workspaces.ids) {
                WorkspaceCell(
                    id = id,
                    focused = id == workspaces.focused,
                    occupied = id in workspaces.occupied,
                    onClick = { context.onCommand(WorkspaceCommand.FocusWorkspace(id)) },
                )
            }
        }
    }

    companion object {
        const val ID = "core.workspaces"
        private const val PERSISTENT = "persistent"
    }
}

@Composable
private fun WorkspaceCell(
    id: Int,
    focused: Boolean,
    occupied: Boolean,
    onClick: () -> Unit,
) {
    val spacing = WhTheme.spacing
    val colors = WhTheme.colors
    val label = WorkspaceBarState.label(id)
    val description =
        stringResource(
            when {
                focused -> R.string.bar_workspace_focused
                occupied -> R.string.bar_workspace_occupied
                else -> R.string.bar_workspace_empty
            },
            label,
        )
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .width(spacing.barWorkspaceWidth)
                .height(spacing.touchTarget)
                .alpha(if (focused || occupied) 1f else EMPTY_ALPHA)
                .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
                .semantics {
                    contentDescription = description
                    selected = focused
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            WhText(label, color = if (focused) colors.accent else colors.foreground, maxLines = 1)
            // El hueco existe aunque no se pinte, para que el número no salte al cambiar de escritorio.
            Box(
                Modifier
                    .width(spacing.barUnderlineWidth)
                    .height(spacing.barUnderlineHeight)
                    .then(if (focused) Modifier.background(colors.accent) else Modifier),
            )
        }
    }
}
