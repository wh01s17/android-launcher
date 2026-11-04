package com.wh01s17.launcher.core.tiling

import kotlin.math.abs

/**
 * `Super + flechas` (plan.md §3): el foco pasa al tile visible más cercano en [direction]. Entre
 * los que están en esa dirección gana el que se solapa en el otro eje con el actual; después, el
 * más cercano; a igualdad, el de menor id (determinista). Sin vecino, nada cambia.
 */
fun Workspace.moveFocus(
    direction: Direction,
    area: Rect,
    gaps: Gaps,
): Workspace = neighborOf(direction, area, gaps)?.let(::focus) ?: this

/** `Super + Shift + flechas`: intercambia el tile con el foco con su vecino; el foco lo sigue. */
fun Workspace.swapInDirection(
    direction: Direction,
    area: Rect,
    gaps: Gaps,
): Workspace {
    val current = focused ?: return this
    val other = neighborOf(direction, area, gaps) ?: return this
    return swap(current, other)
}

private fun Workspace.neighborOf(
    direction: Direction,
    area: Rect,
    gaps: Gaps,
): TileId? {
    val current = focused ?: return null
    val rects = layout(this, area, gaps).visible
    val from = rects[current] ?: return null
    return rects
        .filterKeys { it != current }
        .filterValues { beyond(from, it, direction) }
        .entries
        .sortedWith(
            compareBy<Map.Entry<TileId, Rect>> { !overlapsAcross(from, it.value, direction) }
                .thenBy { gapAlong(from, it.value, direction) }
                .thenBy { offsetAcross(from, it.value, direction) }
                .thenBy { it.key },
        ).firstOrNull()
        ?.key
}

/** El centro de [to] está más allá del de [from] en [direction]. */
private fun beyond(
    from: Rect,
    to: Rect,
    direction: Direction,
): Boolean =
    when (direction) {
        Direction.Left -> to.centerX2 < from.centerX2
        Direction.Right -> to.centerX2 > from.centerX2
        Direction.Up -> to.centerY2 < from.centerY2
        Direction.Down -> to.centerY2 > from.centerY2
    }

private fun overlapsAcross(
    from: Rect,
    to: Rect,
    direction: Direction,
): Boolean =
    if (direction.axis == Orientation.Horizontal) {
        to.top < from.bottom && from.top < to.bottom
    } else {
        to.left < from.right && from.left < to.right
    }

private fun gapAlong(
    from: Rect,
    to: Rect,
    direction: Direction,
): Int =
    when (direction) {
        Direction.Left -> from.left - to.right
        Direction.Right -> to.left - from.right
        Direction.Up -> from.top - to.bottom
        Direction.Down -> to.top - from.bottom
    }.coerceAtLeast(0)

private fun offsetAcross(
    from: Rect,
    to: Rect,
    direction: Direction,
): Int = if (direction.axis == Orientation.Horizontal) abs(to.centerY2 - from.centerY2) else abs(to.centerX2 - from.centerX2)

/**
 * `Super + Ctrl + Shift + flechas` o arrastrar un borde (plan.md §3): el tile con el foco crece
 * [deltaPx] hacia [direction] (o encoge si es negativo), moviendo el borde que comparte con su
 * vecino. En dwindle se ajusta el split más cercano que tenga ese borde; en master, la proporción
 * del principal. Sin borde en esa dirección (está pegado al área), nada cambia.
 */
fun Workspace.resize(
    direction: Direction,
    deltaPx: Int,
    area: Rect,
    gaps: Gaps,
): Workspace {
    val current = focused?.takeIf { fullscreen == null && !isFloating(it) } ?: return this
    val root = root ?: return this
    val outer = area.inset(gaps.outer)
    return when (layout) {
        TilingLayout.Dwindle -> resizeDwindle(root, current, direction, deltaPx, outer, gaps.inner)
        TilingLayout.Master -> resizeMaster(root, current, direction, deltaPx, outer, gaps.inner)
    }
}

private fun Workspace.resizeDwindle(
    root: Node,
    current: TileId,
    direction: Direction,
    deltaPx: Int,
    outer: Rect,
    gap: Int,
): Workspace {
    val placed = root.placeLeaves(outer, gap).first { current in (it.node as Leaf).tiles }
    // El borde de ese lado lo pone el split más cercano cuya orientación cruza la dirección y en el
    // que el tile está del lado correcto (en el primero si crece hacia la derecha o abajo).
    val owner =
        placed.ancestors.firstOrNull { it.split.orientationIn(it.rect) == direction.axis && it.inFirst == direction.positive }
            ?: return this
    val side = (if (direction.axis == Orientation.Horizontal) owner.rect.width else owner.rect.height) - gap
    if (side <= 0) return this
    val change = deltaPx.toFloat() / side * (if (owner.inFirst) 1 else -1)
    val updated = owner.split.copy(ratio = SplitRatio.clamp(owner.split.ratio + change))
    return copy(root = root.replaceNode(owner.split, updated))
}

private fun Workspace.resizeMaster(
    root: Node,
    current: TileId,
    direction: Direction,
    deltaPx: Int,
    outer: Rect,
    gap: Int,
): Workspace {
    val leaves = root.leaves()
    val orientation = Orientation.longSideOf(outer)
    if (leaves.size < 2 || direction.axis != orientation) return this
    val isMaster = current in leaves.first().tiles
    // El principal tiene su borde hacia la derecha/abajo; los apilados, hacia la izquierda/arriba.
    if (isMaster != direction.positive) return this
    val side = (if (orientation == Orientation.Horizontal) outer.width else outer.height) - gap
    if (side <= 0) return this
    val change = deltaPx.toFloat() / side * (if (isMaster) 1 else -1)
    return withMasterRatio(masterRatio + change)
}

/**
 * `Super + J`: invierte la orientación del split que contiene el tile con el foco (deja de ser
 * automática). Sólo en dwindle.
 */
fun Workspace.toggleSplit(
    area: Rect,
    gaps: Gaps,
): Workspace {
    val current = focused ?: return this
    val root = root
    if (layout != TilingLayout.Dwindle || root == null || isFloating(current)) return this
    val parent =
        root
            .placeLeaves(area.inset(gaps.outer), gaps.inner)
            .first { current in (it.node as Leaf).tiles }
            .ancestors
            .firstOrNull()
            ?: return this
    val flipped = parent.split.copy(orientation = parent.split.orientationIn(parent.rect).flipped())
    return copy(root = root.replaceNode(parent.split, flipped))
}
