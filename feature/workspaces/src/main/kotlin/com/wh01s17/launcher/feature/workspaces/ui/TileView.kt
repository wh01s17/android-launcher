package com.wh01s17.launcher.feature.workspaces.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

/**
 * Un tile (plan.md §7 Hito 4): un lanzador vivo con el icono grande y el nombre de la app, borde
 * activo (gradiente) si tiene el foco e inactivo si no, y radio del tema. Un grupo enseña sus
 * pestañas arriba, como la barra de grupos de Hyprland. El contador de notificaciones llega con el
 * acceso a notificaciones (Hito 8).
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
) {
    Column(
        modifier
            .clip(style.shape)
            .background(WhTheme.surfaces.launcher.surface.background)
            .whBorder(active = focused, shape = style.shape, width = style.border),
    ) {
        if (tabs != null) GroupTabs(tabs, shown, onSelectTab)
        TileBody(info, appsLoaded, iconLoader, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun TileBody(
    info: TileInfo?,
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
        Column(
            Modifier.alpha(if (dim) UNAVAILABLE_ALPHA else 1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
        ) {
            if (app != null) AppIcon(app, iconLoader, iconSize, colors.foreground)
            if (showLabel) {
                WhText(
                    app?.label ?: stringResource(R.string.tile_not_installed, info.key.packageName),
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
                    info?.app?.label ?: info?.key?.packageName.orEmpty(),
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

/** Una celda vacía para el mismo hueco mientras se arrastra un tile (deja ver el sitio que deja). */
@Composable
internal fun TileGhost(
    style: TilingStyle,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().whBorder(active = false, shape = style.shape, width = style.border))
}
