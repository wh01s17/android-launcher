package com.wh01s17.launcher.core.tiling

import kotlin.math.roundToInt

/**
 * Un borde que se puede arrastrar en el modo edición (plan.md §7, Hito 4): el hueco entre los dos
 * lados de un split de dwindle, o entre el principal y la pila en master.
 *
 * [orientation] es la del split: `Horizontal` reparte a lo ancho (el borde es una línea vertical y
 * se arrastra en x). [bounds] es el rectángulo que reparte y [line], el hueco entre los dos lados
 * (de grosor 0 si no hay huecos).
 */
data class Divider(
    /** Camino del split desde la raíz (`false` = primer hijo, `true` = segundo); `null` en master. */
    val path: List<Boolean>?,
    val orientation: Orientation,
    val bounds: Rect,
    val line: Rect,
)

/**
 * Los bordes arrastrables de [workspace] tal como [layout] lo distribuye. Ninguno en pantalla
 * completa ni con menos de dos huecos.
 */
fun dividers(
    workspace: Workspace,
    area: Rect,
    gaps: Gaps,
): List<Divider> {
    val root = workspace.root
    if (root == null || root is Leaf || workspace.fullscreen != null) return emptyList()
    val outer = area.inset(gaps.outer)
    return when (workspace.layout) {
        TilingLayout.Dwindle -> root.dividers(outer, gaps.inner, emptyList())
        TilingLayout.Master -> {
            val orientation = Orientation.longSideOf(outer)
            listOf(divider(null, orientation, outer, workspace.masterRatio, gaps.inner))
        }
    }
}

private fun Node.dividers(
    rect: Rect,
    gap: Int,
    path: List<Boolean>,
): List<Divider> {
    if (this !is Split) return emptyList()
    val orientation = orientationIn(rect)
    val (a, b) = splitRect(rect, orientation, ratio, gap)
    return listOf(divider(path, orientation, rect, ratio, gap)) + first.dividers(a, gap, path + false) +
        second.dividers(b, gap, path + true)
}

private fun divider(
    path: List<Boolean>?,
    orientation: Orientation,
    rect: Rect,
    ratio: Float,
    gap: Int,
): Divider {
    val (a, b) = splitRect(rect, orientation, ratio, gap)
    val line =
        if (orientation ==
            Orientation.Horizontal
        ) {
            Rect(a.right, rect.top, b.left, rect.bottom)
        } else {
            Rect(rect.left, a.bottom, rect.right, b.top)
        }
    return Divider(path, orientation, rect, line)
}

/**
 * Lleva [divider] (de [dividers] con este mismo [gaps]) a [positionPx]: la coordenada en su eje
 * (x si el split es horizontal) donde debe quedar el centro del hueco. La proporción se limita a
 * [SplitRatio]; un camino que ya no existe no cambia nada.
 */
fun Workspace.moveDivider(
    divider: Divider,
    positionPx: Int,
    gaps: Gaps,
): Workspace {
    val horizontal = divider.orientation == Orientation.Horizontal
    val start = if (horizontal) divider.bounds.left else divider.bounds.top
    val side = if (horizontal) divider.bounds.width else divider.bounds.height
    val gap = gaps.inner.coerceAtMost(side)
    if (side - gap <= 0) return this
    val ratio = SplitRatio.clamp((positionPx - start - gap / 2f) / (side - gap))
    val path = divider.path ?: return withMasterRatio(ratio)
    val root = root ?: return this
    val split = root.nodeAt(path) as? Split ?: return this
    return copy(root = root.replaceAt(path, split.copy(ratio = ratio)))
}

/** El centro del hueco de [divider] en su eje: lo que [moveDivider] espera para no moverlo. */
val Divider.center: Int
    get() =
        if (orientation == Orientation.Horizontal) {
            ((line.left + line.right) / 2f).roundToInt()
        } else {
            ((line.top + line.bottom) / 2f).roundToInt()
        }

private fun Node.nodeAt(path: List<Boolean>): Node? =
    when {
        path.isEmpty() -> this
        this is Split -> (if (path.first()) second else first).nodeAt(path.drop(1))
        else -> null
    }

private fun Node.replaceAt(
    path: List<Boolean>,
    node: Node,
): Node =
    when {
        path.isEmpty() -> node
        this is Split ->
            if (path.first()) {
                copy(
                    second = second.replaceAt(path.drop(1), node),
                )
            } else {
                copy(first = first.replaceAt(path.drop(1), node))
            }
        else -> this
    }
