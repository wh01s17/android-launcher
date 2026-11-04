package com.wh01s17.launcher.core.tiling

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

/**
 * `resizeactive dx dy` de Hyprland (los atajos `Super + -`/`=` de `bindings/tiling.lua`): mueve el
 * borde derecho del tile con el foco [dxPx] píxeles (positivo hacia la derecha) y el inferior
 * [dyPx]. Si el tile está pegado a ese lado del área, mueve el izquierdo o el superior en su lugar,
 * así que `dx < 0` encoge un tile de la izquierda y agranda uno de la derecha.
 */
fun Workspace.resizeBy(
    dxPx: Int,
    dyPx: Int,
    area: Rect,
    gaps: Gaps,
): Workspace = resizeAxis(Direction.Right, dxPx, area, gaps).resizeAxis(Direction.Down, dyPx, area, gaps)

private fun Workspace.resizeAxis(
    positive: Direction,
    deltaPx: Int,
    area: Rect,
    gaps: Gaps,
): Workspace =
    when {
        deltaPx == 0 -> this
        hasEdge(positive, area, gaps) -> resize(positive, deltaPx, area, gaps)
        else -> resize(if (positive == Direction.Right) Direction.Left else Direction.Up, -deltaPx, area, gaps)
    }

/** El tile con el foco comparte un borde con otro en [direction] (y por tanto [resize] lo movería). */
fun Workspace.hasEdge(
    direction: Direction,
    area: Rect,
    gaps: Gaps,
): Boolean {
    val current = focused?.takeIf { fullscreen == null && !isFloating(it) } ?: return false
    val root = root ?: return false
    val outer = area.inset(gaps.outer)
    return when (layout) {
        TilingLayout.Dwindle -> edgeOwner(root, current, direction, outer, gaps.inner) != null
        TilingLayout.Master -> masterEdge(root, current, direction, outer)
    }
}

/**
 * El split que pone el borde de [current] hacia [direction]: el más cercano cuya orientación cruza
 * la dirección y en el que el tile está del lado correcto (en el primero si va a la derecha o abajo).
 */
private fun edgeOwner(
    root: Node,
    current: TileId,
    direction: Direction,
    outer: Rect,
    gap: Int,
): PlacedSplit? =
    root
        .placeLeaves(outer, gap)
        .first { current in (it.node as Leaf).tiles }
        .ancestors
        .firstOrNull { it.split.orientationIn(it.rect) == direction.axis && it.inFirst == direction.positive }

/** En master sólo hay un borde: el principal lo tiene hacia la derecha/abajo y la pila al revés. */
private fun masterEdge(
    root: Node,
    current: TileId,
    direction: Direction,
    outer: Rect,
): Boolean {
    val leaves = root.leaves()
    return leaves.size >= 2 && direction.axis == Orientation.longSideOf(outer) && (current in leaves.first().tiles) == direction.positive
}

private fun Workspace.resizeDwindle(
    root: Node,
    current: TileId,
    direction: Direction,
    deltaPx: Int,
    outer: Rect,
    gap: Int,
): Workspace {
    val owner = edgeOwner(root, current, direction, outer, gap) ?: return this
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
    if (!masterEdge(root, current, direction, outer)) return this
    val orientation = Orientation.longSideOf(outer)
    val isMaster = current in root.leaves().first().tiles
    val side = (if (orientation == Orientation.Horizontal) outer.width else outer.height) - gap
    if (side <= 0) return this
    val change = deltaPx.toFloat() / side * (if (isMaster) 1 else -1)
    return withMasterRatio(masterRatio + change)
}
