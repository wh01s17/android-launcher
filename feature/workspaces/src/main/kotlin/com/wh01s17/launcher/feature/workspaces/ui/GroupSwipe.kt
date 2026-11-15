package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.wh01s17.launcher.core.tiling.LayoutResult
import com.wh01s17.launcher.core.tiling.TileId

/**
 * Deslizar en horizontal sobre un tile agrupado cambia de pestaña: hacia la izquierda la siguiente,
 * hacia la derecha la anterior. Si no hay pestaña en ese sentido, el gesto no se toca y sigue al
 * carrusel de escritorios. Una pestaña por gesto.
 *
 * Va en el contenedor de la página y no en el tile: el gesto necesita el reparto de toda la página
 * (qué grupo hay bajo el dedo) y, al cambiar de pestaña, los detectores del tile se reinician
 * (`pointerInput(tile)`) a mitad del gesto. El deslizamiento visual lo hace `GroupBody`.
 */
@Composable
internal fun Modifier.groupSwipes(
    layout: () -> LayoutResult?,
    onSelect: (TileId) -> Unit,
): Modifier {
    val latestLayout by rememberUpdatedState(layout)
    val latestSelect by rememberUpdatedState(onSelect)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val group = latestLayout()?.let { groupAt(it, down.position) } ?: return@awaitEachGesture
            var target: TileId? = null
            val drag =
                awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                    target = neighbour(group.first, group.second, if (over < 0) 1 else -1)
                    // Sin vecina no se consume: el carrusel se queda el gesto.
                    if (target != null) change.consume()
                }
            val next = target
            if (drag != null && next != null) {
                latestSelect(next)
                horizontalDrag(drag.id) { it.consume() }
            }
        }
    }
}

/** Las pestañas y la visible del grupo bajo [point], salvo que lo tape un flotante. */
internal fun groupAt(
    result: LayoutResult,
    point: Offset,
): Pair<List<TileId>, TileId>? {
    if (result.floating.values.any { point in it }) return null
    return result.groups.entries
        .firstOrNull { (shown, _) -> result.visible[shown]?.let { point in it } == true }
        ?.let { (shown, tabs) -> tabs to shown }
}

private fun neighbour(
    tabs: List<TileId>,
    shown: TileId,
    step: Int,
): TileId? {
    val index = tabs.indexOf(shown)
    return if (index < 0) null else tabs.getOrNull(index + step)
}

/** El último reparto compuesto de una página; lo lee el gesto al empezar. */
internal class LayoutHolder {
    var result: LayoutResult? = null
}
