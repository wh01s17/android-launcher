package com.wh01s17.launcher.feature.menu.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.icons.AppIcon
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.designsystem.components.WhListRow
import com.wh01s17.launcher.core.designsystem.components.WhListRowSize
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.menu.R

/** Fila de una app: icono, nombre con las letras buscadas en verde y su marca (trabajo, pausa…). */
@Composable
internal fun AppRow(
    item: DrawerItem,
    iconLoader: AppIconLoader,
    onIntent: (DrawerIntent) -> Unit,
) {
    val view = LocalView.current
    // Dónde está el icono en la vista: la app crece desde ahí al abrirse.
    var iconBounds by remember { mutableStateOf<Rect?>(null) }
    WhListRow(
        title = highlighted(item.app.label, item.matchPositions, WhTheme.colors.accent),
        onClick = { onIntent(DrawerIntent.Launch(item.app, iconBounds?.let { LaunchOrigin(view, it.toAndroidRect()) })) },
        onLongClick = { onIntent(DrawerIntent.ShowMenu(item.app)) },
        leading = { DrawerIcon(item.app, iconLoader, Modifier.onGloballyPositioned { iconBounds = it.boundsInRoot() }) },
        trailingText = item.badge?.let { badgeText(it) },
        size = WhListRowSize.Menu,
    )
}

/** Menú contextual: atajos de la app, info y desinstalar, sobre un velo. */
@Composable
internal fun AppMenuOverlay(
    menu: AppMenu,
    iconLoader: AppIconLoader,
    onIntent: (DrawerIntent) -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WhScrim(onClick = { onIntent(DrawerIntent.DismissMenu) }, clickLabel = stringResource(R.string.drawer_close))
        WhPanel(Modifier.width(WhTheme.spacing.menuWidth), contentPadding = WhTheme.spacing.controlGap) {
            Row(
                Modifier.padding(horizontal = WhTheme.spacing.rowPaddingX),
                horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrawerIcon(menu.app, iconLoader)
                WhText(menu.app.label, style = WhTheme.typography.heading, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column {
                menu.shortcuts.forEach { shortcut ->
                    WhListRow(
                        title = shortcut.shortLabel,
                        onClick = { onIntent(DrawerIntent.LaunchShortcut(shortcut)) },
                        enabled = shortcut.enabled,
                        leadingIcon = WhIcons.ExternalLink,
                    )
                }
                if (!menu.shortcutsAvailable) {
                    WhText(
                        stringResource(R.string.drawer_shortcuts_unavailable),
                        Modifier.padding(horizontal = WhTheme.spacing.rowPaddingX, vertical = WhTheme.spacing.controlPaddingY),
                        style = WhTheme.typography.caption,
                        color = WhTheme.colors.darkForeground,
                    )
                }
                WhListRow(
                    stringResource(R.string.drawer_action_info),
                    { onIntent(DrawerIntent.OpenInfo(menu.app)) },
                    leadingIcon = WhIcons.Info,
                )
                if (menu.app.uninstallable) {
                    WhListRow(
                        stringResource(R.string.drawer_action_uninstall),
                        { onIntent(DrawerIntent.Uninstall(menu.app)) },
                        leadingIcon = WhIcons.Trash,
                    )
                }
            }
        }
    }
}

@Composable
private fun badgeText(badge: DrawerBadge): String =
    when (badge) {
        DrawerBadge.ProfilePaused -> stringResource(R.string.drawer_profile_paused)
        DrawerBadge.Suspended -> stringResource(R.string.drawer_suspended)
        DrawerBadge.WorkProfile -> stringResource(R.string.drawer_work_profile)
    }

/** [text] con las posiciones de [positions] en [color]; las que no existen se ignoran. */
internal fun highlighted(
    text: String,
    positions: IntArray,
    color: Color,
): AnnotatedString =
    buildAnnotatedString {
        append(text)
        val style = SpanStyle(color = color)
        positions.filter { it in text.indices }.forEach { addStyle(style, it, it + 1) }
    }

/** Icono de la lista: grande y teñido con el color del texto. */
@Composable
private fun DrawerIcon(
    app: AppEntry,
    loader: AppIconLoader,
    modifier: Modifier = Modifier,
) = AppIcon(app, loader, WhTheme.spacing.iconSizeLarge, WhTheme.colors.foreground, modifier)
