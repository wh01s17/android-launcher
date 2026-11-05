package com.wh01s17.launcher.feature.notifications

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.notifications.NotificationItem

/**
 * La primera notificación de [items] que no estaba en [previous]; `null` si no hay nueva o si es la
 * primera lectura ([previous] `null`): al arrancar no se enseñan como nuevas las que ya estaban.
 */
internal fun newestSince(
    previous: Set<String>?,
    items: List<NotificationItem>,
): NotificationItem? = previous?.let { known -> items.firstOrNull { it.key !in known } }

/**
 * Aviso emergente de una notificación recién llegada, con la barra de cuenta atrás de
 * `shell.notifications.toml` (`countdown`). Dura [seconds] y se va solo; tocarlo abre la notificación.
 * Con [enabled] en `false` (`notifications.toast`) no enseña nada.
 */
@Composable
fun NotificationToastHost(
    items: List<NotificationItem>,
    enabled: Boolean,
    seconds: Int,
    labelOf: (NotificationItem) -> String,
    onOpen: (NotificationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf<NotificationItem?>(null) }
    var known by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(items, enabled) {
        val fresh = newestSince(known, items)
        known = items.map { it.key }.toSet()
        if (enabled && fresh != null) shown = fresh
        if (!enabled) shown = null
    }
    shown?.let { item ->
        NotificationToast(item, labelOf(item), seconds, { onOpen(item) }, { shown = null }, modifier)
    }
}

@Composable
internal fun NotificationToast(
    item: NotificationItem,
    label: String,
    seconds: Int,
    onOpen: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = WhTheme.surfaces.notification
    val remaining = remember(item.key) { Animatable(1f) }
    LaunchedEffect(item.key, seconds) {
        remaining.snapTo(1f)
        remaining.animateTo(0f, tween(seconds * MILLIS, easing = LinearEasing))
        onDone()
    }
    WhPanel(
        modifier
            .padding(WhTheme.spacing.controlGap)
            .fillMaxWidth()
            .clickable(role = Role.Button) {
                onOpen()
                onDone()
            },
        style = style.surface,
        contentPadding = WhTheme.spacing.controlGap,
    ) {
        WhText(label, style = WhTheme.typography.caption, color = WhTheme.colors.darkForeground)
        WhText(item.title ?: item.text.orEmpty(), color = style.surface.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (item.title != null && !item.text.isNullOrBlank()) {
            WhText(
                item.text.orEmpty(),
                style = WhTheme.typography.bodySmall,
                color = WhTheme.colors.darkForeground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.fillMaxWidth().height(WhTheme.spacing.barUnderlineHeight), contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth(remaining.value).height(WhTheme.spacing.barUnderlineHeight).background(style.countdown))
        }
    }
}

private const val MILLIS = 1000
