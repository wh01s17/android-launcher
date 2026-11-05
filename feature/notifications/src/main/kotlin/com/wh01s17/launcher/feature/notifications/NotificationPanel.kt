package com.wh01s17.launcher.feature.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.notifications.NotificationItem
import com.wh01s17.launcher.core.system.notifications.NotificationState

/** Cuánto de la altura disponible puede ocupar el panel. */
private const val MAX_HEIGHT_FRACTION = 0.7f

/**
 * El panel de notificaciones propio (plan.md §7 Hito 8), con la superficie de
 * `shell.notifications.toml`. Va bajo la barra: quien lo usa le pasa en [modifier] el margen de arriba.
 * Sin acceso a notificaciones explica para qué sirve y manda a los ajustes de Android (R8).
 */
@Composable
fun NotificationPanel(
    state: NotificationState,
    labelOf: (NotificationItem) -> String,
    now: Long,
    onOpen: (NotificationItem) -> Unit,
    onDismiss: (NotificationItem) -> Unit,
    onClearAll: () -> Unit,
    onGrantAccess: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        WhScrim(onClick = onClose, clickLabel = stringResource(R.string.notifications_close))
        WhPanel(
            Modifier
                .align(Alignment.TopCenter)
                .padding(WhTheme.spacing.controlGap)
                .fillMaxWidth()
                .heightIn(max = maxHeight * MAX_HEIGHT_FRACTION),
            style = WhTheme.surfaces.notification.surface,
            contentPadding = WhTheme.spacing.controlGap,
        ) {
            Header(state, onClearAll)
            when {
                !state.access -> AccessPrompt(onGrantAccess)
                state.items.isEmpty() -> WhText(stringResource(R.string.notifications_empty), color = WhTheme.colors.darkForeground)
                else ->
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
                        items(state.items, key = { it.key }) { item ->
                            NotificationRow(item, labelOf(item), now, { onOpen(item) }, { onDismiss(item) })
                        }
                    }
            }
        }
    }
}

@Composable
private fun Header(
    state: NotificationState,
    onClearAll: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            WhText(stringResource(R.string.notifications_title), style = WhTheme.typography.title, color = WhTheme.colors.accent)
            if (state.access) {
                val count = state.items.size
                WhText(pluralStringResource(R.plurals.notifications_count, count, count), color = WhTheme.colors.darkForeground)
            }
        }
        if (state.items.any { it.clearable }) {
            WhButton(onClearAll) { WhText(stringResource(R.string.notifications_clear)) }
        }
    }
}

@Composable
private fun AccessPrompt(onGrantAccess: () -> Unit) {
    WhText(stringResource(R.string.notifications_access_title), style = WhTheme.typography.body, color = WhTheme.colors.yellow)
    WhText(stringResource(R.string.notifications_access_body), color = WhTheme.colors.foreground)
    WhButton(onGrantAccess) { WhText(stringResource(R.string.notifications_access_grant)) }
}

@Composable
private fun NotificationRow(
    item: NotificationItem,
    label: String,
    now: Long,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = WhTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = WhTheme.spacing.controlGap / 2)) {
            WhText(
                "$label · ${ageText(NotificationAge.of(now, item.postTime))}",
                style = WhTheme.typography.caption,
                color = colors.darkForeground,
            )
            WhText(
                item.title ?: stringResource(R.string.notifications_untitled),
                color = colors.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.text.isNullOrBlank()) {
                WhText(
                    item.text.orEmpty(),
                    style = WhTheme.typography.bodySmall,
                    color = colors.darkForeground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (item.clearable) {
            val description = stringResource(R.string.notifications_dismiss, label)
            WhButton(onDismiss) { WhIcon(WhIcons.Close, contentDescription = description) }
        }
    }
}

@Composable
internal fun ageText(age: NotificationAge): String =
    when (age) {
        NotificationAge.Now -> stringResource(R.string.notifications_age_now)
        is NotificationAge.Minutes -> stringResource(R.string.notifications_age_minutes, age.value)
        is NotificationAge.Hours -> stringResource(R.string.notifications_age_hours, age.value)
        is NotificationAge.Days -> stringResource(R.string.notifications_age_days, age.value)
    }
