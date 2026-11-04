package com.wh01s17.launcher.feature.menu.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.icons.AppIcon
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.designsystem.components.WhListRow
import com.wh01s17.launcher.core.designsystem.components.WhListRowSize
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.menu.R

/** Fila de una app: icono, nombre con las letras buscadas en verde y su marca (trabajo, pausa…). */
@Composable
internal fun AppRow(
    item: DrawerItem,
    iconLoader: AppIconLoader,
    onIntent: (SearchIntent) -> Unit,
    selected: Boolean = false,
) {
    val view = LocalView.current
    // Dónde está el icono en la vista: la app crece desde ahí al abrirse.
    var iconBounds by remember { mutableStateOf<Rect?>(null) }
    WhListRow(
        title = highlighted(item.app.label, item.matchPositions, WhTheme.colors.accent),
        selected = selected,
        keyboardSelected = selected,
        onClick = { onIntent(SearchIntent.Launch(item.app, iconBounds?.let { LaunchOrigin(view, it.toAndroidRect()) })) },
        onLongClick = { onIntent(SearchIntent.ShowMenu(item.app)) },
        leading = { DrawerIcon(item.app, iconLoader, Modifier.onGloballyPositioned { iconBounds = it.boundsInRoot() }) },
        trailingText = item.badge?.let { badgeText(it) },
        size = WhListRowSize.Menu,
    )
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
internal fun DrawerIcon(
    app: AppEntry,
    loader: AppIconLoader,
    modifier: Modifier = Modifier,
) = AppIcon(app, loader, WhTheme.spacing.iconSizeLarge, WhTheme.colors.foreground, modifier)
