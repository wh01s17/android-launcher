package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.modifier.whBorder
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.tiling.Divider
import com.wh01s17.launcher.core.tiling.Gaps
import com.wh01s17.launcher.core.tiling.Orientation
import com.wh01s17.launcher.core.tiling.Rect
import com.wh01s17.launcher.core.tiling.Workspace
import com.wh01s17.launcher.core.tiling.center
import com.wh01s17.launcher.core.tiling.dividers
import com.wh01s17.launcher.feature.workspaces.R
import com.wh01s17.launcher.feature.workspaces.WorkspacesIntent
import com.wh01s17.launcher.feature.workspaces.model.Desk
import kotlin.math.roundToInt

/** Proporción de la asa de un borde: larga como un tercio de su área táctil, fina como el borde. */
private const val GRIP_LENGTH_FACTOR = 0.75f

/**
 * Asas sobre los bordes compartidos (plan.md §7 Hito 4: "redimensionar arrastrando bordes"). Cada
 * una es un área táctil a lo largo del hueco con una pastilla verde en medio.
 */
@Composable
internal fun DividerHandles(
    workspace: Workspace,
    area: Rect,
    gaps: Gaps,
    onIntent: (WorkspacesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = remember(workspace, area, gaps) { dividers(workspace, area, gaps) }
    for (divider in list) {
        // La clave es el split, no su posición: así el gesto no se reinicia mientras el borde se mueve.
        key(divider.path, divider.orientation) { DividerHandle(divider, onIntent, modifier) }
    }
}

@Composable
private fun DividerHandle(
    divider: Divider,
    onIntent: (WorkspacesIntent) -> Unit,
    modifier: Modifier,
) {
    val spacing = WhTheme.spacing
    val thickness = with(LocalDensity.current) { (spacing.touchTarget / 2).roundToPx() }
    val vertical = divider.orientation == Orientation.Horizontal
    val handle = handleRect(divider, thickness)
    val latest by rememberUpdatedState(divider)
    val latestHandle by rememberUpdatedState(handle)
    val label = stringResource(R.string.edit_hint)
    Box(
        modifier
            .placeAt(handle)
            .semantics { contentDescription = label }
            .pointerInput(divider.path, divider.orientation) {
                // El asa se mueve con el borde: se trabaja con la posición absoluta del dedo (asa + local)
                // menos dónde se agarró, para que no salte ni se quede atrás.
                var grab = 0f
                detectDragGestures(
                    onDragStart = { touch -> grab = absolute(latestHandle, touch.x, touch.y, vertical) - latest.center },
                ) { change, _ ->
                    change.consume()
                    val position = absolute(latestHandle, change.position.x, change.position.y, vertical) - grab
                    onIntent(WorkspacesIntent.DragDivider(latest, position.roundToInt()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val length = spacing.touchTarget * GRIP_LENGTH_FACTOR
        val width = spacing.controlGap / 2
        Box(
            Modifier
                .size(if (vertical) width else length, if (vertical) length else width)
                .background(WhTheme.colors.accent, CircleShape),
        )
    }
}

private fun absolute(
    handle: Rect,
    x: Float,
    y: Float,
    vertical: Boolean,
): Float = if (vertical) handle.left + x else handle.top + y

/** El área táctil del borde: [thickness] de ancho centrada en el hueco, a todo lo largo. */
private fun handleRect(
    divider: Divider,
    thickness: Int,
): Rect {
    val center = divider.center
    val half = thickness / 2
    val line = divider.line
    return if (divider.orientation == Orientation.Horizontal) {
        Rect(center - half, line.top, center - half + thickness, line.bottom)
    } else {
        Rect(line.left, center - half, line.right, center - half + thickness)
    }
}

/**
 * Tira de escritorios que aparece arriba mientras se arrastra un tile: soltarlo sobre un número lo
 * lleva a ese escritorio (el equivalente a soltarlo en `[n]` de la barra, que vive en otro módulo).
 */
@Composable
internal fun DropStrip(
    current: Int,
    hovered: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = WhTheme.colors
    Row(
        modifier
            .background(WhTheme.surfaces.launcher.surface.background, WhTheme.borders.shape)
            .whBorder(active = hovered != null, alpha = WhTheme.surfaces.launcher.surface.borderAlpha),
    ) {
        for (id in Desk.FIRST..Desk.LAST) {
            val label = workspaceLabel(id)
            val description = stringResource(R.string.tile_drop_here, label)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (id == hovered) Modifier.background(WhTheme.surfaces.launcher.selectedBackground) else Modifier)
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                WhText(
                    "[$label]",
                    color =
                        when (id) {
                            hovered -> colors.accent
                            current -> colors.darkForeground
                            else -> colors.foreground
                        },
                    maxLines = 1,
                )
            }
        }
    }
}

/** Esquina para cambiar el tamaño de un tile flotante: un punto verde en el área táctil. */
@Composable
internal fun ResizeGrip(modifier: Modifier = Modifier) {
    val spacing = WhTheme.spacing
    Box(modifier.size(spacing.touchTarget / 2), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .padding(spacing.controlGap / 2)
                .size(spacing.controlGap)
                .background(WhTheme.colors.accent, CircleShape),
        )
    }
}

/**
 * Mientras se arrastra un tile: dónde quedará al soltarlo. La mitad del destino de ese lado, o el
 * destino entero con «agrupar» si cae en su centro.
 */
@Composable
internal fun DropHint(
    group: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .background(WhTheme.surfaces.launcher.selectedBackground, WhTheme.borders.shape)
            .whBorder(active = true, shape = WhTheme.borders.shape),
        contentAlignment = Alignment.Center,
    ) {
        if (group) WhText(stringResource(R.string.tile_drop_group), color = WhTheme.colors.accent)
    }
}
