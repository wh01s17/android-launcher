package com.wh01s17.launcher.core.tiling

/**
 * El árbol de un workspace (plan.md §5.3–5.4). Un [Leaf] es un hueco del mosaico; si tiene varios
 * tiles es un grupo (pestañas, como los grupos de Hyprland) y sólo se ve [Leaf.shown]. Un [Split]
 * reparte su rectángulo entre dos hijos.
 */
sealed interface Node {
    /** Tiles del subárbol en orden (izquierda/arriba primero). */
    fun tiles(): List<TileId>

    fun leaves(): List<Leaf>
}

data class Leaf(
    val tiles: List<TileId>,
    val active: Int = 0,
) : Node {
    init {
        require(tiles.isNotEmpty()) { "a leaf needs at least one tile" }
        require(active in tiles.indices) { "active $active out of ${tiles.size}" }
        require(tiles.toSet().size == tiles.size) { "duplicate tiles in a group: $tiles" }
    }

    val shown: TileId get() = tiles[active]
    val isGroup: Boolean get() = tiles.size > 1

    override fun tiles(): List<TileId> = tiles

    override fun leaves(): List<Leaf> = listOf(this)

    companion object {
        fun of(tile: TileId) = Leaf(listOf(tile))
    }
}

/**
 * [orientation] `null` = automática por el lado largo de su rectángulo en cada distribución (dwindle
 * de Hyprland); `toggleSplit` la fija. [ratio] es la parte de [first].
 */
data class Split(
    val first: Node,
    val second: Node,
    val ratio: Float = SplitRatio.HALF,
    val orientation: Orientation? = null,
) : Node {
    init {
        require(ratio in SplitRatio.MIN..SplitRatio.MAX) { "ratio $ratio out of range" }
    }

    override fun tiles(): List<TileId> = first.tiles() + second.tiles()

    override fun leaves(): List<Leaf> = first.leaves() + second.leaves()

    fun orientationIn(rect: Rect): Orientation = orientation ?: Orientation.longSideOf(rect)
}

/** El nodo con [leaf] sustituido por [replacement] (comparando por identidad de contenido). */
internal fun Node.replaceLeaf(
    leaf: Leaf,
    replacement: Node,
): Node =
    when (this) {
        is Leaf -> if (this == leaf) replacement else this
        is Split -> copy(first = first.replaceLeaf(leaf, replacement), second = second.replaceLeaf(leaf, replacement))
    }

/** El nodo sin [leaf]: su padre se sustituye por el hermano. `null` si [leaf] era todo el árbol. */
internal fun Node.removeLeaf(leaf: Leaf): Node? =
    when (this) {
        is Leaf -> if (this == leaf) null else this
        is Split ->
            when {
                first == leaf -> second
                second == leaf -> first
                // Sólo un hijo que *es* la hoja puede desaparecer, y ese caso ya está arriba.
                else -> copy(first = checkNotNull(first.removeLeaf(leaf)), second = checkNotNull(second.removeLeaf(leaf)))
            }
    }

internal fun Node.leafOf(tile: TileId): Leaf? = leaves().firstOrNull { tile in it.tiles }

/** El nodo con [old] (un split o una hoja) sustituido por [new]. */
internal fun Node.replaceNode(
    old: Node,
    new: Node,
): Node =
    when {
        this == old -> new
        this is Split -> copy(first = first.replaceNode(old, new), second = second.replaceNode(old, new))
        else -> this
    }

/** Un nodo con su rectángulo y su cadena de antepasados (del más cercano al raíz). */
internal data class Placed(
    val node: Node,
    val rect: Rect,
    val ancestors: List<PlacedSplit>,
)

internal data class PlacedSplit(
    val split: Split,
    val rect: Rect,
    /** `true` si el nodo de abajo está en [Split.first]. */
    val inFirst: Boolean,
)

/** Recorre el árbol en dwindle, con el rectángulo de cada hoja y el camino hasta ella. */
internal fun Node.placeLeaves(
    rect: Rect,
    gap: Int,
    ancestors: List<PlacedSplit> = emptyList(),
): List<Placed> =
    when (this) {
        is Leaf -> listOf(Placed(this, rect, ancestors))
        is Split -> {
            val (a, b) = splitRect(rect, orientationIn(rect), ratio, gap)
            first.placeLeaves(a, gap, listOf(PlacedSplit(this, rect, inFirst = true)) + ancestors) +
                second.placeLeaves(b, gap, listOf(PlacedSplit(this, rect, inFirst = false)) + ancestors)
        }
    }
