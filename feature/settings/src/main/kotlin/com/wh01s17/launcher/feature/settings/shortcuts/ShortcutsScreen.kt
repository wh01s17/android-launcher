package com.wh01s17.launcher.feature.settings.shortcuts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.feature.settings.R

/**
 * `Super + K` (plan.md §3 y §6.4): la tabla de atajos generada desde `bindings`, con lo que hay que
 * pulsar en este equipo y los que no se pueden pulsar, con su motivo. Con [onRowClick], cada fila
 * se puede editar (Ajustes → Atajos).
 */
@Composable
fun ShortcutsScreen(
    table: ShortcutTable?,
    modifier: Modifier = Modifier,
    onRowClick: ((ShortcutRow) -> Unit)? = null,
    header: @Composable () -> Unit = {},
) {
    val spacing = WhTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(spacing.panelPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
    ) {
        item { Intro(table) }
        item { header() }
        if (table == null) return@LazyColumn
        item { SectionTitle(stringResource(R.string.shortcuts_section_keys)) }
        if (table.keys.isEmpty()) item { WhText(stringResource(R.string.shortcuts_empty), color = WhTheme.colors.darkForeground) }
        items(table.keys, key = { it.trigger.toString() }) { ShortcutLine(it, onRowClick) }
        item { SectionTitle(stringResource(R.string.shortcuts_section_gestures)) }
        if (table.gestures.isEmpty()) item { WhText(stringResource(R.string.shortcuts_empty), color = WhTheme.colors.darkForeground) }
        items(table.gestures, key = { it.trigger.toString() }) { ShortcutLine(it, onRowClick) }
    }
}

@Composable
private fun Intro(table: ShortcutTable?) {
    val colors = WhTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
        WhText(stringResource(R.string.shortcuts_title), style = WhTheme.typography.display, color = colors.accent)
        WhText(stringResource(R.string.shortcuts_intro), color = colors.darkForeground)
        table ?: return@Column
        val alternative = table.superAlternative
        val (text, color) =
            when {
                table.superDelivered -> stringResource(R.string.shortcuts_super_delivered) to colors.accent
                alternative != null -> stringResource(R.string.shortcuts_super_alternative, alternative.configName) to colors.cyan
                else -> stringResource(R.string.shortcuts_super_none) to colors.yellow
            }
        WhText(text, color = color)
    }
}

@Composable
private fun SectionTitle(title: String) {
    WhText(
        "-- $title --",
        style = WhTheme.typography.caption,
        color = WhTheme.colors.darkForeground,
        modifier = Modifier.padding(top = WhTheme.spacing.panelGap),
    )
}

@Composable
private fun ShortcutLine(
    row: ShortcutRow,
    onClick: ((ShortcutRow) -> Unit)?,
) {
    val colors = WhTheme.colors
    val blocked = row.blocked
    val press =
        when (val trigger = row.trigger) {
            is BindingTrigger.Keys -> row.press
            is BindingTrigger.Touch -> stringResource(gestureLabel(trigger.gesture))
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick(row) } else Modifier)
                .padding(vertical = WhTheme.spacing.controlPaddingY),
        horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            WhText(press, color = if (blocked == null) colors.cyan else colors.darkForeground)
            if (blocked != null) WhText(stringResource(blockLabel(blocked)), style = WhTheme.typography.caption, color = colors.yellow)
        }
        WhText(
            commandLabel(row.command),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = if (blocked == null) colors.foreground else colors.darkForeground,
        )
    }
}
