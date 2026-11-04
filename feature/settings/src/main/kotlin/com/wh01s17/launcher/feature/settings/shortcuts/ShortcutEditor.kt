package com.wh01s17.launcher.feature.settings.shortcuts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.config.BindingEdit
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.core.input.binding.KeyChord
import com.wh01s17.launcher.feature.settings.R

/**
 * Editor de un atajo (plan.md §7 Hito 6). La actividad le pasa las teclas con [EditorKey] (el foco de
 * Compose no es fiable en modo táctil: una tecla podía perderse) y guarda [captured]; [onEdit] guarda.
 * Un gesto no se captura: sólo se puede quitar. Si la combinación ya era de otro atajo, se avisa: al
 * guardar, ese otro la pierde.
 */
@Composable
fun ShortcutEditor(
    row: ShortcutRow,
    table: ShortcutTable,
    captured: KeyChord?,
    onEdit: (BindingEdit) -> Unit,
    onDismiss: () -> Unit,
) {
    val isKeys = row.trigger is BindingTrigger.Keys
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WhScrim(onClick = onDismiss)
        WhPanel(Modifier.padding(WhTheme.spacing.panelPadding).widthIn(max = WhTheme.spacing.menuWidth)) {
            EditorBody(row, table, captured, isKeys)
            EditorButtons(row, captured, isKeys, onEdit, onDismiss)
        }
    }
}

@Composable
private fun EditorBody(
    row: ShortcutRow,
    table: ShortcutTable,
    captured: KeyChord?,
    isKeys: Boolean,
) {
    val colors = WhTheme.colors
    WhText(commandLabel(row.command), style = WhTheme.typography.title, color = colors.accent)
    WhText(stringResource(R.string.shortcuts_editor_current, row.press), color = colors.darkForeground)
    if (!isKeys) {
        WhText(stringResource(R.string.shortcuts_editor_gesture), color = colors.darkForeground)
        return
    }
    WhText(
        captured?.toString() ?: stringResource(R.string.shortcuts_editor_press),
        style = WhTheme.typography.heading,
        color = if (captured == null) colors.darkForeground else colors.cyan,
        modifier = Modifier.padding(vertical = WhTheme.spacing.panelGap),
    )
    val owner = captured?.let { chord -> table.keys.firstOrNull { (it.trigger as BindingTrigger.Keys).chord == chord && it != row } }
    if (owner != null) WhText(stringResource(R.string.shortcuts_editor_taken, commandLabel(owner.command)), color = colors.yellow)
}

@Composable
private fun EditorButtons(
    row: ShortcutRow,
    captured: KeyChord?,
    isKeys: Boolean,
    onEdit: (BindingEdit) -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        Modifier.padding(top = WhTheme.spacing.panelGap),
        horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
    ) {
        if (isKeys) {
            WhButton(
                onClick = { captured?.let { onEdit(BindingEdit.Rebind(row.trigger, BindingTrigger.Keys(it))) } },
                enabled = captured != null,
            ) { WhText(stringResource(R.string.shortcuts_editor_save)) }
        }
        WhButton(onClick = { onEdit(BindingEdit.Remove(row.trigger)) }) { WhText(stringResource(R.string.shortcuts_editor_remove)) }
        WhButton(onClick = onDismiss) { WhText(stringResource(R.string.shortcuts_editor_cancel)) }
    }
}
