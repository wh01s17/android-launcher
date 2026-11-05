package com.wh01s17.launcher.core.tiling

/**
 * Arrastrar una ventana sobre otra en Hyprland: [tile] sale de su hueco (lo ocupa su hermano) y
 * parte el de [target] por el lado [side], así que queda justo ahí y los demás no se mueven. No es
 * un intercambio: el destino se queda donde estaba y comparte su hueco con el que llega.
 *
 * El split nuevo fija su orientación (no se recalcula al girar la pantalla): es donde se soltó. En
 * master equivale a ponerlo justo antes ([Direction.Left]/[Direction.Up]) o después del destino en
 * la pila. Si [target] es un grupo, queda junto al grupo entero; si [tile] estaba en un grupo, sólo
 * sale esa pestaña. Recibe el foco. Mismo tile, flotantes o desconocidos: sin cambios.
 */
fun Workspace.moveNextTo(
    tile: TileId,
    target: TileId,
    side: Direction,
): Workspace {
    val root = root ?: return this
    if (tile == target || root.leafOf(tile) == null || root.leafOf(target) == null) return this
    val rest = checkNotNull(root.withoutTile(tile)) { "$target keeps the tree alive" }
    val destination = checkNotNull(rest.leafOf(target))
    val moved = Leaf.of(tile)
    val before = side == Direction.Left || side == Direction.Up
    val orientation = if (side == Direction.Left || side == Direction.Right) Orientation.Horizontal else Orientation.Vertical
    val split = Split(if (before) moved else destination, if (before) destination else moved, orientation = orientation)
    return copy(root = rest.replaceLeaf(destination, split), fullscreen = null).focus(tile)
}
