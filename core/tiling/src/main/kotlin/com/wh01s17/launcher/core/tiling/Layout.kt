package com.wh01s17.launcher.core.tiling

/** Algoritmo de mosaico (plan.md §5.4). */
enum class TilingLayout(
    val configName: String,
) {
    /** Árbol binario: cada tile nuevo parte el que tiene el foco por su lado largo. */
    Dwindle("dwindle"),

    /** Un tile principal (`masterRatio`) y el resto apilado; en vertical, el principal arriba. */
    Master("master"),
    ;

    companion object {
        fun fromConfig(name: String?): TilingLayout? = entries.firstOrNull { it.configName == name }
    }
}

/** Rectángulo de un tile flotante como fracción del área (sobrevive a girar la pantalla). */
data class FloatingBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    init {
        require(width > 0f && height > 0f && width <= 1f && height <= 1f) { "invalid floating size: $this" }
    }

    /** En píxeles dentro de [area], siempre dentro de ella aunque las fracciones se salgan. */
    fun toRect(area: Rect): Rect {
        val w = (area.width * width).toInt().coerceIn(0, area.width)
        val h = (area.height * height).toInt().coerceIn(0, area.height)
        val left = (area.left + area.width * x).toInt().coerceIn(area.left, area.right - w)
        val top = (area.top + area.height * y).toInt().coerceIn(area.top, area.bottom - h)
        return Rect(left, top, left + w, top + h)
    }

    companion object {
        /** Al hacerse flotante, un tile aparece centrado con el 60 % del área. */
        val Default = FloatingBounds(x = 0.2f, y = 0.2f, width = 0.6f, height = 0.6f)
    }
}

data class FloatingTile(
    val id: TileId,
    val bounds: FloatingBounds = FloatingBounds.Default,
)

/**
 * Resultado de distribuir un workspace: dónde va cada tile visible. En un grupo sólo aparece el
 * tile visible ([groups] dice cuáles comparten hueco); los flotantes van encima, en [floating].
 * Con [fullscreen], ese tile ocupa toda el área útil y no se ve ninguno más.
 */
data class LayoutResult(
    val tiled: Map<TileId, Rect>,
    val floating: Map<TileId, Rect>,
    /** Tile visible → todos los tiles de su grupo (sólo para grupos de más de uno). */
    val groups: Map<TileId, List<TileId>>,
    val fullscreen: TileId?,
) {
    /** Todo lo que se ve, flotantes al final (se dibujan encima). */
    val visible: Map<TileId, Rect> get() = tiled + floating

    companion object {
        val Empty = LayoutResult(emptyMap(), emptyMap(), emptyMap(), null)
    }
}

/** Distribuye [workspace] en [area] (pantalla menos barra e insets) con [gaps]. Determinista. */
fun layout(
    workspace: Workspace,
    area: Rect,
    gaps: Gaps,
): LayoutResult {
    val outer = area.inset(gaps.outer)
    val fullscreen = workspace.fullscreen
    if (fullscreen != null) return LayoutResult(mapOf(fullscreen to outer), emptyMap(), emptyMap(), fullscreen)
    val root = workspace.root
    val tiled =
        when {
            root == null -> emptyList()
            workspace.layout == TilingLayout.Dwindle -> root.placeLeaves(outer, gaps.inner).map { it.node as Leaf to it.rect }
            else -> placeMaster(root.leaves(), outer, gaps.inner, workspace.masterRatio)
        }
    return LayoutResult(
        tiled = tiled.associate { (leaf, rect) -> leaf.shown to rect },
        floating = workspace.floating.associate { it.id to it.bounds.toRect(outer) },
        groups = tiled.filter { it.first.isGroup }.associate { (leaf, _) -> leaf.shown to leaf.tiles },
        fullscreen = null,
    )
}

/** El master va a la izquierda en apaisado y arriba en vertical; el resto se reparte a partes iguales. */
internal fun placeMaster(
    leaves: List<Leaf>,
    rect: Rect,
    gap: Int,
    masterRatio: Float,
): List<Pair<Leaf, Rect>> {
    if (leaves.size == 1) return listOf(leaves.single() to rect)
    val orientation = Orientation.longSideOf(rect)
    val (master, stack) = splitRect(rect, orientation, masterRatio, gap)
    val stacked = stackRects(stack, orientation.flipped(), leaves.size - 1, gap)
    return listOf(leaves.first() to master) + leaves.drop(1).zip(stacked)
}
