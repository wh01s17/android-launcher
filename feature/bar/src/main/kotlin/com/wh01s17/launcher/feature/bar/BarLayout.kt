package com.wh01s17.launcher.feature.bar

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints

/** Los tres grupos de la barra (plan.md §5.5), de izquierda a derecha. */
internal enum class BarGroup { Left, Center, Right }

/**
 * Lo que un módulo cuenta de sí a la barra mientras se pinta. [collapsible]: está en reposo (metrónomo
 * parado, media en pausa) y puede ocultarse si los módulos no caben juntos. Es estado: la barra lo lee
 * al medir, así que cambiarlo sólo vuelve a colocar, no recompone.
 */
@Stable
internal class BarSlotState {
    var collapsible by mutableStateOf(false)
}

/** El [BarSlotState] del módulo que se está pintando; `null` fuera de la barra (p. ej. en un panel). */
internal val LocalBarSlot = staticCompositionLocalOf<BarSlotState?> { null }

/** `true` mientras la barra está en modo compacto porque sus módulos no caben con el relleno normal. */
internal val LocalBarCompact = compositionLocalOf { false }

/**
 * La posición de un módulo en la barra. [anchor] es el módulo que se mantiene centrado en la pantalla
 * (`centerAnchor`); [state], lo que el módulo dice de sí (si puede ocultarse).
 */
internal data class BarSlot(
    val group: BarGroup,
    val anchor: Boolean = false,
    val state: BarSlotState? = null,
)

/**
 * El ancho de un módulo ya medido. Uno de ancho 0 (que no pinta nada) no reserva hueco. Uno
 * [collapsible] se puede ocultar si no cabe todo.
 */
internal data class SlotSpec(
    val group: BarGroup,
    val width: Int,
    val anchor: Boolean,
    val collapsible: Boolean = false,
)

/** El ancho que necesitan todos los módulos juntos: los tres grupos, con sus huecos, sin pisarse. */
internal fun requiredWidth(
    slots: List<SlotSpec>,
    itemGap: Int,
    groupGap: Int,
): Int {
    val groups = BarGroup.entries.map { group -> slots.filter { it.group == group && it.width > 0 } }.filter { it.isNotEmpty() }
    val spans = groups.sumOf { group -> group.sumOf { it.width } + itemGap * (group.size - 1) }
    return spans + groupGap * (groups.size - 1).coerceAtLeast(0)
}

/**
 * Los índices de los módulos que se dejan de pintar para que el resto quepa en [totalWidth]: los
 * [SlotSpec.collapsible], empezando por el de más a la derecha, sólo los necesarios. Si ni así cabe,
 * se ocultan todos los que se pueden.
 */
internal fun collapsedSlots(
    slots: List<SlotSpec>,
    totalWidth: Int,
    itemGap: Int,
    groupGap: Int,
): Set<Int> {
    val hidden = mutableSetOf<Int>()
    val candidates = slots.indices.filter { slots[it].collapsible && slots[it].width > 0 }.sortedByDescending { screenOrder(slots, it) }
    for (i in candidates) {
        val visible = slots.mapIndexed { j, spec -> if (j in hidden) spec.copy(width = 0) else spec }
        if (requiredWidth(visible, itemGap, groupGap) <= totalWidth) break
        hidden += i
    }
    return hidden
}

/** Orden de izquierda a derecha en pantalla: por grupo y, dentro del grupo, por su posición. */
private fun screenOrder(
    slots: List<SlotSpec>,
    index: Int,
): Int = slots[index].group.ordinal * slots.size + index

/**
 * Coordenada x de cada módulo. Izquierda y derecha se pegan a su borde; el centro se centra en la
 * pantalla —o, con `centerAnchor`, deja centrado el módulo ancla—, pero sin pisar a los vecinos
 * (deja al menos [groupGap] con ellos): si no cabe donde toca, se desplaza hacia el espacio libre, y
 * si no hay espacio se queda pegado a la izquierda. Los módulos del mismo grupo mantienen su orden,
 * separados por [itemGap] (cada módulo ya lleva su relleno, así que normalmente 0). Si el centro
 * pisa [notch] (el recorte de la cámara, en x de la barra), se aparta a su derecha o, si no cabe, a
 * su izquierda.
 */
internal fun placeSlots(
    slots: List<SlotSpec>,
    totalWidth: Int,
    itemGap: Int,
    groupGap: Int,
    notch: IntRange? = null,
): List<Int> {
    val positions = IntArray(slots.size)
    val byGroup = BarGroup.entries.associateWith { group -> slots.indices.filter { slots[it].group == group && slots[it].width > 0 } }

    fun span(indices: List<Int>) = indices.sumOf { slots[it].width } + itemGap * (indices.size - 1).coerceAtLeast(0)

    fun place(
        indices: List<Int>,
        start: Int,
    ) {
        var x = start
        for (i in indices) {
            positions[i] = x
            x += slots[i].width + itemGap
        }
    }

    val left = byGroup.getValue(BarGroup.Left)
    val center = byGroup.getValue(BarGroup.Center)
    val right = byGroup.getValue(BarGroup.Right)
    place(left, 0)
    val rightStart = totalWidth - span(right)
    place(right, rightStart)

    val centerWidth = span(center)
    val anchor = center.indexOfFirst { slots[it].anchor }
    val desired =
        if (anchor >= 0) {
            val before = span(center.take(anchor)) + if (anchor > 0) itemGap else 0
            totalWidth / 2 - (before + slots[center[anchor]].width / 2)
        } else {
            (totalWidth - centerWidth) / 2
        }
    val minStart = if (left.isEmpty()) 0 else span(left) + groupGap
    val maxStart = (if (right.isEmpty()) totalWidth else rightStart - groupGap) - centerWidth
    val start = if (minStart <= maxStart) desired.coerceIn(minStart, maxStart) else minStart
    place(center, avoidNotch(start, centerWidth, notch, groupGap, minStart..maxStart))
    return positions.toList()
}

/**
 * [start] del grupo central, movido fuera de [notch] si lo pisa. Junto al recorte no hace falta hueco
 * y, para caber, los rellenos de los módulos (medio [gap] cada uno) pueden tocarse con los vecinos:
 * los textos siguen separados.
 */
private fun avoidNotch(
    start: Int,
    width: Int,
    notch: IntRange?,
    gap: Int,
    allowed: IntRange,
): Int {
    val overlaps = notch != null && width > 0 && start <= notch.last && start + width > notch.first
    val right = (notch?.last ?: 0) + 1
    val left = (notch?.first ?: 0) - width
    val relaxed = gap + gap / 2
    val room = (allowed.first - relaxed)..(allowed.last + relaxed)
    return when {
        !overlaps -> start
        right in room -> right
        left in room -> left
        else -> start
    }
}

/**
 * Cuándo la barra pasa a modo compacto y cuándo vuelve. Pasa en cuanto los módulos no caben con el
 * relleno normal; al medir ya en compacto, apunta cuánto ahorró, y vuelve cuando el ancho compacto más
 * ese ahorro cabe otra vez. Si el ahorro cambió (entró o salió un módulo), como mucho rebota una vez.
 */
internal class BarCompaction {
    var compact by mutableStateOf(false)
        private set

    /** El modo con que se compusieron los módulos que se están midiendo. */
    var composedCompact = false
    private var normalWidth = 0
    private var savings: Int? = null

    fun update(
        required: Int,
        available: Int,
    ) {
        if (!composedCompact) {
            if (required > available) {
                normalWidth = required
                savings = null
                compact = true
            }
            return
        }
        val saved = savings ?: (normalWidth - required).coerceAtLeast(0).also { savings = it }
        if (compact && required + saved <= available) compact = false
    }
}

/**
 * Coloca los módulos de la barra. Cada hijo lleva su [BarSlot] como `layoutId`; el alto lo marca
 * la barra y los módulos se centran en él. Si no caben juntos, primero se compactan ([LocalBarCompact])
 * y, si aún no caben, se dejan de pintar los que están en reposo ([collapsedSlots]): nunca se pisan.
 */
@Composable
internal fun BarLayout(
    itemGapPx: Int,
    groupGapPx: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // El recorte de la cámara que cae dentro de la barra, en x de la barra (plan.md §5.5).
    val view = LocalView.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    var notch by remember { mutableStateOf<IntRange?>(null) }
    val compaction = remember { BarCompaction() }
    Layout(
        content = {
            val compact = compaction.compact
            compaction.composedCompact = compact
            CompositionLocalProvider(LocalBarCompact provides compact, content = content)
        },
        modifier =
            modifier.onGloballyPositioned { coordinates ->
                origin = coordinates.positionInWindow()
                notch = topCutout(view, origin, coordinates.size.height)
            },
    ) { measurables, constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
        val placeables = measurables.map { it.measure(loose) }
        val slots = measurables.map { it.layoutId as? BarSlot ?: BarSlot(BarGroup.Left) }
        val specs =
            slots.mapIndexed { i, slot ->
                SlotSpec(slot.group, placeables[i].width, slot.anchor, slot.state?.collapsible == true)
            }
        compaction.update(requiredWidth(specs, itemGapPx, groupGapPx), constraints.maxWidth)
        val hidden = collapsedSlots(specs, constraints.maxWidth, itemGapPx, groupGapPx)
        val visible = specs.mapIndexed { i, spec -> if (i in hidden) spec.copy(width = 0) else spec }
        val xs = placeSlots(visible, constraints.maxWidth, itemGapPx, groupGapPx, notch)
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else placeables.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { i, placeable ->
                if (i !in hidden) placeable.placeRelative(xs[i], (height - placeable.height) / 2)
            }
        }
    }
}

/** Las x (relativas a la barra en [origin], de alto [height]) del recorte superior que la cruza. */
private fun topCutout(
    view: View,
    origin: Offset,
    height: Int,
): IntRange? {
    val rect = view.rootWindowInsets?.displayCutout?.boundingRectTop ?: return null
    if (rect.isEmpty || rect.bottom <= origin.y || rect.top >= origin.y + height) return null
    return (rect.left - origin.x.toInt())..(rect.right - origin.x.toInt() - 1)
}

/** Marca un módulo con su posición para [BarLayout]. */
internal fun Modifier.barSlot(slot: BarSlot): Modifier = layoutId(slot)
