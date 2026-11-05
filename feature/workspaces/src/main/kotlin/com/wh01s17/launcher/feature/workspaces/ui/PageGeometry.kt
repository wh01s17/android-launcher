package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.wh01s17.launcher.core.tiling.Direction
import com.wh01s17.launcher.core.tiling.FloatingBounds
import com.wh01s17.launcher.core.tiling.Rect
import com.wh01s17.launcher.core.tiling.TileId
import kotlin.math.roundToInt

/** Coloca el elemento exactamente en [rect] (píxeles del lienzo), como lo calcula el motor. */
internal fun Modifier.placeAt(rect: Rect): Modifier =
    layout { measurable, _ ->
        val placeable = measurable.measure(Constraints.fixed(rect.width, rect.height))
        layout(rect.width, rect.height) { placeable.place(rect.left, rect.top) }
    }

internal operator fun Rect.contains(point: Offset): Boolean = point.x >= left && point.x < right && point.y >= top && point.y < bottom

/**
 * Un tile que se arrastra en el modo edición. El gesto sigue en el tile original (que se queda
 * quieto como hueco); encima se pinta [preview], así las coordenadas del dedo no se mueven con él.
 */
internal data class TileDrag(
    val tile: TileId,
    val rect: Rect,
    val floating: Boolean,
    /** Arrastrando la esquina de un flotante: cambia el tamaño en vez de moverlo. */
    val resize: Boolean,
    /** Punto de contacto inicial, en píxeles del lienzo. */
    val start: Offset,
    val offset: Offset = Offset.Zero,
) {
    val pointer: Offset get() = start + offset

    /** Dónde se pinta ahora: un flotante no sale de [outer] ni encoge por debajo de [minSize]. */
    fun preview(
        outer: Rect,
        minSize: Int,
    ): Rect {
        val dx = offset.x.roundToInt()
        val dy = offset.y.roundToInt()
        return when {
            resize ->
                Rect(
                    rect.left,
                    rect.top,
                    (rect.right + dx).coerceIn(minOf(rect.left + minSize, outer.right), outer.right),
                    (rect.bottom + dy).coerceIn(minOf(rect.top + minSize, outer.bottom), outer.bottom),
                )
            floating -> {
                val left = (rect.left + dx).coerceIn(outer.left, (outer.right - rect.width).coerceAtLeast(outer.left))
                val top = (rect.top + dy).coerceIn(outer.top, (outer.bottom - rect.height).coerceAtLeast(outer.top))
                Rect(left, top, left + rect.width, top + rect.height)
            }
            else -> Rect(rect.left + dx, rect.top + dy, rect.right + dx, rect.bottom + dy)
        }
    }
}

/** [rect] como fracción de [outer], para guardarlo en un tile flotante. */
internal fun floatingBoundsOf(
    rect: Rect,
    outer: Rect,
): FloatingBounds? {
    if (outer.width <= 0 || outer.height <= 0) return null
    if (rect.width <= 0 || rect.height <= 0) return null
    return FloatingBounds(
        x = (rect.left - outer.left).toFloat() / outer.width,
        y = (rect.top - outer.top).toFloat() / outer.height,
        width = (rect.width.toFloat() / outer.width).coerceAtMost(1f),
        height = (rect.height.toFloat() / outer.height).coerceAtMost(1f),
    )
}

/** La celda de la tira de escritorios (1–10) bajo [point], o `null` si está fuera de la tira. */
internal fun stripCellAt(
    point: Offset,
    width: Int,
    stripHeight: Int,
    count: Int,
): Int? {
    if (point.y >= stripHeight || point.y < 0 || width <= 0) return null
    return (point.x / width * count).toInt().coerceIn(0, count - 1) + 1
}

/** Lo que hace soltar un tile sobre otro: ponerlo a un lado ([Side]) o agruparlos ([Group]). */
internal sealed interface DropZone {
    data object Group : DropZone

    data class Side(
        val side: Direction,
    ) : DropZone
}

/** El tile sobre el que se soltaría, la zona y el rectángulo a resaltar mientras se arrastra. */
internal data class DropTarget(
    val tile: TileId,
    val zone: DropZone,
    val highlight: Rect,
)

/**
 * Dónde caería [drag] entre los tiles del mosaico ([tiled]): la mitad central de un tile (un cuarto
 * de margen por lado) agrupa; fuera de ella, el borde más cercano dice a qué lado queda, como al
 * arrastrar una ventana en Hyprland. Fuera de todo tile (o sobre sí mismo), `null`.
 */
internal fun dropTargetAt(
    point: Offset,
    dragged: TileId,
    tiled: Map<TileId, Rect>,
): DropTarget? {
    val (tile, rect) = tiled.entries.firstOrNull { (other, rect) -> other != dragged && point in rect } ?: return null
    val zone = dropZone(rect, point)
    return DropTarget(tile, zone, highlightOf(rect, zone))
}

internal fun dropZone(
    rect: Rect,
    point: Offset,
): DropZone {
    val center =
        Rect(
            rect.left + rect.width / CENTER_MARGIN,
            rect.top + rect.height / CENTER_MARGIN,
            rect.right - rect.width / CENTER_MARGIN,
            rect.bottom - rect.height / CENTER_MARGIN,
        )
    if (point in center) return DropZone.Group
    // Distancia a cada borde en proporción al lado: en un tile alargado no gana siempre el lado corto.
    val distances =
        mapOf(
            Direction.Left to (point.x - rect.left) / rect.width,
            Direction.Right to (rect.right - point.x) / rect.width,
            Direction.Up to (point.y - rect.top) / rect.height,
            Direction.Down to (rect.bottom - point.y) / rect.height,
        )
    return DropZone.Side(distances.minBy { it.value }.key)
}

/** Lo que ocuparía el tile al soltarlo: la mitad del destino de ese lado, o el destino entero si agrupa. */
private fun highlightOf(
    rect: Rect,
    zone: DropZone,
): Rect =
    when (zone) {
        DropZone.Group -> rect
        is DropZone.Side ->
            when (zone.side) {
                Direction.Left -> Rect(rect.left, rect.top, rect.left + rect.width / 2, rect.bottom)
                Direction.Right -> Rect(rect.left + rect.width / 2, rect.top, rect.right, rect.bottom)
                Direction.Up -> Rect(rect.left, rect.top, rect.right, rect.top + rect.height / 2)
                Direction.Down -> Rect(rect.left, rect.top + rect.height / 2, rect.right, rect.bottom)
            }
    }

private const val CENTER_MARGIN = 4
