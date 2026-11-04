package com.wh01s17.launcher.core.tiling

/**
 * Un workspace (plan.md §5.3): el árbol de tiles, los flotantes, el foco y el modo. Es inmutable:
 * cada operación (`insert`, `remove`, `moveFocus`…) devuelve uno nuevo, así que deshacer o
 * comparar estados es trivial y el motor se prueba sin Android.
 *
 * Invariantes (se comprueban al crearlo): cada tile aparece una sola vez entre árbol y flotantes;
 * [focused] y [fullscreen] son tiles del workspace o `null`; [history] sólo tiene tiles del
 * workspace, sin repetir y sin el del foco; [masterRatio] está en el rango de [SplitRatio] y
 * [maxTiles] es al menos 1.
 */
data class Workspace(
    val root: Node? = null,
    val layout: TilingLayout = TilingLayout.Dwindle,
    val focused: TileId? = null,
    val floating: List<FloatingTile> = emptyList(),
    val fullscreen: TileId? = null,
    val masterRatio: Float = DEFAULT_MASTER_RATIO,
    /** Huecos del mosaico (no tiles): por encima, un tile nuevo se agrupa con el del foco (plan.md §5.4). */
    val maxTiles: Int = DEFAULT_MAX_TILES,
    /**
     * Tiles que tuvieron el foco antes, el más reciente al final. Al cerrar el tile con foco, éste
     * vuelve al último que siga existiendo, como `focus_on_close` de Hyprland.
     */
    val history: List<TileId> = emptyList(),
) {
    /** Todos los tiles: los del árbol en orden y después los flotantes. */
    val tiles: List<TileId> = root?.tiles().orEmpty() + floating.map { it.id }

    init {
        require(tiles.toSet().size == tiles.size) { "a tile appears twice: $tiles" }
        require(focused == null || focused in tiles) { "focused $focused is not in the workspace" }
        require(fullscreen == null || fullscreen in tiles) { "fullscreen $fullscreen is not in the workspace" }
        require(masterRatio in SplitRatio.MIN..SplitRatio.MAX) { "masterRatio $masterRatio out of range" }
        require(maxTiles >= 1) { "maxTiles must be at least 1" }
        require(history.toSet().size == history.size && history.all { it in tiles } && focused !in history) {
            "invalid focus history $history (focused $focused)"
        }
    }

    val isEmpty: Boolean get() = tiles.isEmpty()

    /** Huecos ocupados del mosaico (un grupo cuenta como uno). */
    val slots: Int get() = root?.leaves()?.size ?: 0

    operator fun contains(tile: TileId): Boolean = tile in tiles

    fun isFloating(tile: TileId): Boolean = floating.any { it.id == tile }

    /** Foco en [tile]; si está en un grupo, pasa a ser la pestaña visible. Desconocido: sin cambios. */
    fun focus(tile: TileId): Workspace {
        if (tile !in tiles) return this
        val leaf = root?.leafOf(tile)
        val root = if (leaf != null) root.replaceLeaf(leaf, leaf.copy(active = leaf.tiles.indexOf(tile))) else root
        val previous = focused?.takeIf { it != tile }
        return copy(root = root, focused = tile, history = history - tile - listOfNotNull(previous).toSet() + listOfNotNull(previous))
    }

    fun withLayout(layout: TilingLayout): Workspace = copy(layout = layout)

    fun withMasterRatio(ratio: Float): Workspace = copy(masterRatio = SplitRatio.clamp(ratio))

    companion object {
        /** `mfact` de Hyprland (plan.md §5.4). */
        const val DEFAULT_MASTER_RATIO = 0.55f

        /** Límite práctico en un teléfono (plan.md §5.4), configurable. */
        const val DEFAULT_MAX_TILES = 8
    }
}
