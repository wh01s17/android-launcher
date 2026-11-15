package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.min
import com.wh01s17.launcher.core.apps.icons.AppIcon
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.modifier.whBorder
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.feature.workspaces.R
import com.wh01s17.launcher.feature.workspaces.TileInfo

/** Fracción del lado corto del tile que ocupa el icono, y su tope en múltiplos de `iconSizeLarge`. */
private const val ICON_FRACTION = 0.4f
private const val MAX_ICON_FACTOR = 3

/** Opacidad de una app que no se puede abrir ahora (perfil en pausa, suspendida o desinstalada). */
private const val UNAVAILABLE_ALPHA = 0.45f

/** Duración del deslizamiento al cambiar de pestaña en un grupo (curva `easeOutQuint`, como los escritorios). */
private const val TAB_SLIDE_MILLIS = 260

/**
 * Un tile (plan.md §7 Hito 4): un lanzador vivo con el icono grande y el nombre de la app, borde
 * activo (gradiente) si tiene el foco e inactivo si no, y radio del tema. Un grupo enseña sus
 * pestañas arriba, como la barra de grupos de Hyprland. Con el acceso a notificaciones (Hito 8) el
 * icono lleva una insignia con las notificaciones de esa app ([LocalTileBadges]).
 */
@Composable
internal fun TileView(
    info: TileInfo?,
    appsLoaded: Boolean,
    focused: Boolean,
    tabs: List<Pair<TileId, TileInfo?>>?,
    shown: TileId,
    style: TilingStyle,
    iconLoader: AppIconLoader,
    onSelectTab: (TileId) -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    onLongPress: () -> Unit = {},
) {
    Column(
        modifier
            .clip(style.shape)
            .background(WhTheme.surfaces.launcher.surface.background)
            .whBorder(active = focused, shape = style.shape, width = style.border)
            // TalkBack: el tile con foco se anuncia como seleccionado (el borde activo no se oye).
            .semantics { selected = focused },
    ) {
        val body = Modifier.fillMaxWidth().weight(1f)
        if (tabs == null) {
            Body(info, appsLoaded, iconLoader, editing, onLongPress, body)
        } else {
            GroupTabs(tabs, shown, onSelectTab)
            GroupBody(tabs, shown, info, appsLoaded, iconLoader, editing, onLongPress, body)
        }
    }
}

@Composable
private fun Body(
    info: TileInfo?,
    appsLoaded: Boolean,
    iconLoader: AppIconLoader,
    editing: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier,
) {
    when (info) {
        is TileInfo.Widget -> WidgetBody(info, editing, onLongPress, modifier)
        is TileInfo.Native -> NativeBody(info, editing, modifier)
        else -> TileBody(info as TileInfo.App?, appsLoaded, iconLoader, modifier)
    }
}

/**
 * El contenido de un grupo: al cambiar de pestaña (swipe o toque en la pestaña) la nueva entra
 * deslizándose por el lado al que se va y la anterior sale por el otro, como pasar de escritorio.
 * Las pestañas de arriba se quedan quietas.
 */
@Composable
private fun GroupBody(
    tabs: List<Pair<TileId, TileInfo?>>,
    shown: TileId,
    shownInfo: TileInfo?,
    appsLoaded: Boolean,
    iconLoader: AppIconLoader,
    editing: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier,
) {
    val order = tabs.map { it.first }
    AnimatedContent(
        targetState = shown,
        modifier = modifier.clipToBounds(),
        transitionSpec = {
            // A la pestaña siguiente (swipe a la izquierda) la nueva entra por la derecha.
            val direction = if (order.indexOf(targetState) >= order.indexOf(initialState)) 1 else -1
            val spec = tween<IntOffset>(TAB_SLIDE_MILLIS, easing = EaseOutQuint)
            slideInHorizontally(spec) { width -> width * direction } togetherWith
                slideOutHorizontally(spec) { width -> -width * direction }
        },
        label = "group-tab",
    ) { tab ->
        // La pestaña que sale puede no estar ya en el grupo (se quitó): se pinta vacía mientras sale.
        val info = if (tab == shown) shownInfo else tabs.firstOrNull { it.first == tab }?.second
        Body(info, appsLoaded, iconLoader, editing, onLongPress, Modifier.fillMaxSize())
    }
}

/** Un widget: lo pinta la app ([LocalWidgetRenderer]); sin ella, sólo su nombre genérico. */
@Composable
private fun WidgetBody(
    info: TileInfo.Widget,
    editing: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier,
) {
    val renderer = LocalWidgetRenderer.current
    if (renderer != null) {
        renderer.Content(info.appWidgetId, interactive = !editing, onLongPress = onLongPress, modifier = modifier)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            WhText(stringResource(R.string.tile_widget), style = WhTheme.typography.bodySmall, color = WhTheme.colors.darkForeground)
        }
    }
}

@Composable
private fun NativeBody(
    info: TileInfo.Native,
    editing: Boolean,
    modifier: Modifier,
) {
    val renderer = LocalWidgetRenderer.current
    if (renderer != null) {
        renderer.Native(info.id, interactive = !editing, modifier = modifier)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            WhText(info.id, style = WhTheme.typography.bodySmall, color = WhTheme.colors.darkForeground)
        }
    }
}

@Composable
private fun TileBody(
    info: TileInfo.App?,
    appsLoaded: Boolean,
    iconLoader: AppIconLoader,
    modifier: Modifier,
) {
    val spacing = WhTheme.spacing
    val colors = WhTheme.colors
    BoxWithConstraints(modifier.padding(spacing.controlGap), contentAlignment = Alignment.Center) {
        val app = info?.app
        // Mientras se lee el índice de apps no se sabe aún si falta: se deja el tile vacío.
        if (info == null || (app == null && !appsLoaded)) return@BoxWithConstraints
        val iconSize = iconSize(min(maxWidth, maxHeight))
        val dim = app == null || info.unavailable
        // El nombre sólo si cabe debajo del icono.
        val showLabel = maxHeight >= iconSize + spacing.rowHeight
        val label = app?.label ?: stringResource(R.string.tile_not_installed, info.key.packageName)
        Column(
            Modifier
                .alpha(if (dim) UNAVAILABLE_ALPHA else 1f)
                // Sin el nombre a la vista, TalkBack lo lee igual (si no, sólo diría «abrir»).
                .then(if (showLabel) Modifier else Modifier.semantics { contentDescription = label }),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
        ) {
            if (app != null) {
                val badge = if (info.unavailable) 0 else LocalTileBadges.current.count(info.key)
                Box {
                    AppIcon(app, iconLoader, iconSize, colors.foreground)
                    if (badge > 0) TileBadge(badge, Modifier.align(BadgeAlignment))
                }
            }
            if (showLabel) {
                WhText(
                    label,
                    style = WhTheme.typography.bodySmall,
                    color = colors.foreground,
                    textAlign = TextAlign.Center,
                    maxLines = if (app == null) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun iconSize(shortSide: Dp): Dp {
    val base = WhTheme.spacing.iconSizeLarge
    return (shortSide * ICON_FRACTION).coerceIn(base, base * MAX_ICON_FACTOR)
}

/** Pestañas de un grupo: la visible en verde con su subrayado, como el escritorio activo de la barra. */
@Composable
private fun GroupTabs(
    tabs: List<Pair<TileId, TileInfo?>>,
    shown: TileId,
    onSelect: (TileId) -> Unit,
) {
    val spacing = WhTheme.spacing
    val colors = WhTheme.colors
    Row(Modifier.fillMaxWidth().height(spacing.rowHeight).background(colors.lighterBackground)) {
        for ((tile, info) in tabs) {
            val active = tile == shown
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource = interaction, indication = null, role = Role.Tab) { onSelect(tile) },
                contentAlignment = Alignment.Center,
            ) {
                WhText(
                    tabLabel(info),
                    Modifier.padding(horizontal = spacing.controlGap),
                    style = WhTheme.typography.caption,
                    color = if (active) colors.accent else colors.darkForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (active) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(spacing.barUnderlineHeight)
                            .background(colors.accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun tabLabel(info: TileInfo?): String =
    when (info) {
        is TileInfo.App -> info.app?.label ?: info.key.packageName
        is TileInfo.Widget -> stringResource(R.string.tile_widget)
        is TileInfo.Native -> info.id
        null -> ""
    }

/** Una celda vacía para el mismo hueco mientras se arrastra un tile (deja ver el sitio que deja). */
@Composable
internal fun TileGhost(
    style: TilingStyle,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().whBorder(active = false, shape = style.shape, width = style.border))
}
