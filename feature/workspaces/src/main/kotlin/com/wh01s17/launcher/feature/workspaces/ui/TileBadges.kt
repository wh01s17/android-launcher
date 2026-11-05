package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.modifier.whBorder
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.workspaces.R

/**
 * Cuántas notificaciones tiene cada app (plan.md §7 Hito 8). Lo pone la app con [LocalTileBadges]
 * desde el acceso a notificaciones; sin él, ningún tile lleva insignia.
 */
fun interface TileBadges {
    fun count(app: AppKey): Int
}

val LocalTileBadges = compositionLocalOf { TileBadges { 0 } }

/** Por encima de esto se escribe `99+`. */
internal const val MAX_BADGE = 99

/** La insignia de notificaciones de un tile: el número en verde sobre el fondo de selección. */
@Composable
internal fun TileBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val colors = WhTheme.colors
    val shape = WhTheme.borders.shape
    val description = pluralStringResource(R.plurals.tile_notifications, count, count)
    WhText(
        if (count > MAX_BADGE) "$MAX_BADGE+" else count.toString(),
        modifier
            .background(colors.selectionBackground, shape)
            .whBorder(active = false, shape = shape, width = WhTheme.surfaces.notification.surface.borderWidth)
            .padding(horizontal = WhTheme.spacing.controlGap / 2)
            .semantics { contentDescription = description },
        style = WhTheme.typography.caption,
        color = colors.accent,
    )
}

/** Alineación de la insignia en la esquina superior derecha del icono. */
internal val BadgeAlignment = Alignment.TopEnd
