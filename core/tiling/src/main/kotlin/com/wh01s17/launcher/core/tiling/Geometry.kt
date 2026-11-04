package com.wh01s17.launcher.core.tiling

import kotlin.math.roundToInt

/** Identidad de un tile. Estable entre versiones (plan.md §8.3): se guarda en `launcher.json`. */
@JvmInline
value class TileId(
    val value: Long,
) : Comparable<TileId> {
    override fun compareTo(other: TileId): Int = value.compareTo(other.value)

    override fun toString() = "#$value"
}

/** Rectángulo en píxeles enteros, `[left, right) × [top, bottom)`. */
data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(right >= left && bottom >= top) { "empty-negative rect: $this" }
    }

    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val area: Long get() = width.toLong() * height

    /** Centro ×2, para comparar sin fracciones. */
    val centerX2: Int get() = left + right
    val centerY2: Int get() = top + bottom

    /** Encogido [px] por cada lado, sin llegar a tamaño negativo. */
    fun inset(px: Int): Rect {
        val dx = px.coerceAtMost(width / 2)
        val dy = px.coerceAtMost(height / 2)
        return Rect(left + dx, top + dy, right - dx, bottom - dy)
    }

    fun overlaps(other: Rect): Boolean = left < other.right && other.left < right && top < other.bottom && other.top < bottom

    fun contains(other: Rect): Boolean = other.left >= left && other.top >= top && other.right <= right && other.bottom <= bottom
}

/** Cómo reparte un split: [Horizontal] pone los hijos lado a lado; [Vertical], uno encima del otro. */
enum class Orientation {
    Horizontal,
    Vertical,
    ;

    fun flipped(): Orientation = if (this == Horizontal) Vertical else Horizontal

    companion object {
        /** Por el lado largo, como dwindle de Hyprland sin `preserve_split`. */
        fun longSideOf(rect: Rect): Orientation = if (rect.width >= rect.height) Horizontal else Vertical
    }
}

enum class Direction {
    Left,
    Right,
    Up,
    Down,
    ;

    /** El eje en el que se mueve: izquierda/derecha cruzan splits horizontales. */
    val axis: Orientation get() = if (this == Left || this == Right) Orientation.Horizontal else Orientation.Vertical

    /** `true` si va hacia valores mayores de la coordenada (derecha, abajo). */
    val positive: Boolean get() = this == Right || this == Down
}

/**
 * Huecos en píxeles (plan.md §4.5). [inner] es la distancia **entre** dos tiles (en Hyprland,
 * `gaps_in` se aplica a cada ventana, así que entre dos hay el doble); [outer], el margen al borde
 * del área.
 */
data class Gaps(
    val inner: Int = 0,
    val outer: Int = 0,
) {
    init {
        require(inner >= 0 && outer >= 0) { "gaps must be >= 0: $this" }
    }
}

/** Proporción de un split, siempre entre [MIN] y [MAX] (plan.md §5.4). */
object SplitRatio {
    const val MIN = 0.1f
    const val MAX = 0.9f
    const val HALF = 0.5f

    fun clamp(ratio: Float): Float = if (ratio.isNaN()) HALF else ratio.coerceIn(MIN, MAX)
}

/**
 * Parte [rect] en dos según [orientation], dejando [gap] entre ellas. Los píxeles se reparten
 * enteros y sin perder ninguno: primero + hueco + segundo = el lado original. Si el lado es más
 * corto que el hueco, el hueco se reduce.
 */
internal fun splitRect(
    rect: Rect,
    orientation: Orientation,
    ratio: Float,
    gap: Int,
): Pair<Rect, Rect> {
    val side = if (orientation == Orientation.Horizontal) rect.width else rect.height
    val usedGap = gap.coerceAtMost(side)
    val available = side - usedGap
    val first = (available * ratio).roundToInt().coerceIn(0, available)
    return if (orientation == Orientation.Horizontal) {
        Rect(rect.left, rect.top, rect.left + first, rect.bottom) to Rect(rect.left + first + usedGap, rect.top, rect.right, rect.bottom)
    } else {
        Rect(rect.left, rect.top, rect.right, rect.top + first) to Rect(rect.left, rect.top + first + usedGap, rect.right, rect.bottom)
    }
}

/**
 * Parte [rect] en [count] trozos iguales a lo largo de [orientation] con [gap] entre ellos. El resto
 * de la división entera va al último, para cubrir el lado exacto.
 */
internal fun stackRects(
    rect: Rect,
    orientation: Orientation,
    count: Int,
    gap: Int,
): List<Rect> {
    require(count > 0)
    val side = if (orientation == Orientation.Horizontal) rect.width else rect.height
    val usedGap = gap.coerceAtMost(side / count)
    val available = side - usedGap * (count - 1)
    val each = available / count
    return List(count) { i ->
        val start = i * (each + usedGap)
        val end = if (i == count - 1) side else start + each
        if (orientation == Orientation.Horizontal) {
            Rect(rect.left + start, rect.top, rect.left + end, rect.bottom)
        } else {
            Rect(rect.left, rect.top + start, rect.right, rect.top + end)
        }
    }
}
