package com.wh01s17.launcher.core.tiling

/*
 * Grupos explícitos, como en Hyprland (`bindings/tiling.lua`): un hueco del mosaico con varias
 * pestañas. Sólo se crean a petición; `insert` nunca agrupa.
 */

/**
 * `moveintogroup`: [tile] sale de su hueco y entra en el de [target] como la pestaña visible (si
 * [target] estaba solo, el hueco pasa a ser un grupo). Recibe el foco. Flotantes o el mismo hueco:
 * sin cambios.
 */
fun Workspace.groupWith(
    tile: TileId,
    target: TileId,
): Workspace {
    val root = root ?: return this
    val from = root.leafOf(tile)
    val to = root.leafOf(target)
    if (from == null || to == null || from == to) return this
    val without = checkNotNull(root.withoutTile(tile)) { "$target keeps the tree alive" }
    val destination = checkNotNull(without.leafOf(target))
    val joined = Leaf(destination.tiles + tile, destination.tiles.size)
    return copy(root = without.replaceLeaf(destination, joined), fullscreen = null).focus(tile)
}

/** `Super + Alt + flechas`: mete el tile con el foco en el grupo (o tile) vecino en [direction]. */
fun Workspace.moveIntoGroup(
    direction: Direction,
    area: Rect,
    gaps: Gaps,
): Workspace {
    val tile = focused ?: return this
    val neighbor = neighborOf(direction, area, gaps) ?: return this
    return groupWith(tile, neighbor)
}

/**
 * `Super + Alt + G`: saca [tile] de su grupo a un hueco propio junto al grupo (a la derecha o
 * debajo, como un tile nuevo). Fuera de un grupo: sin cambios.
 */
fun Workspace.moveOutOfGroup(tile: TileId? = focused): Workspace {
    val root = root ?: return this
    val leaf = tile?.let(root::leafOf) ?: return this
    if (!leaf.isGroup) return this
    val rest = checkNotNull(root.withoutTile(tile))
    val group = checkNotNull(rest.leafOf(leaf.tiles.first { it != tile }))
    return copy(root = rest.replaceLeaf(group, Split(group, Leaf.of(tile))), fullscreen = null).focus(tile)
}

/**
 * `Super + G`: si el tile con el foco está en un grupo, el grupo se deshace y cada pestaña pasa a
 * su propio hueco; si no, se agrupa con el último tile enfocado de otro hueco (enfocar uno y luego
 * otro, y agrupar). Sin a quién unirse, nada cambia.
 */
fun Workspace.toggleGroup(): Workspace {
    val root = root ?: return this
    val tile = focused ?: return this
    val leaf = root.leafOf(tile) ?: return this
    if (leaf.isGroup) {
        val separate = leaf.tiles.drop(1).fold<TileId, Node>(Leaf.of(leaf.tiles.first())) { acc, next -> Split(acc, Leaf.of(next)) }
        return copy(root = root.replaceLeaf(leaf, separate))
    }
    val partner = history.lastOrNull { it != tile && root.leafOf(it)?.let { other -> other != leaf } == true } ?: return this
    return groupWith(tile, partner)
}
