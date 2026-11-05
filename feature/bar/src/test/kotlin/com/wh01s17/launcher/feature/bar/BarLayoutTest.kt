package com.wh01s17.launcher.feature.bar

import org.junit.Assert.assertEquals
import org.junit.Test

class BarLayoutTest {
    private fun spec(
        group: BarGroup,
        width: Int,
        anchor: Boolean = false,
    ) = SlotSpec(group, width, anchor)

    private val total = 400
    private val gap = 10

    @Test
    fun `left and right stick to their edges in order`() {
        val xs =
            placeSlots(
                listOf(spec(BarGroup.Left, 40), spec(BarGroup.Left, 60), spec(BarGroup.Right, 30), spec(BarGroup.Right, 50)),
                total,
                gap,
                gap,
            )
        assertEquals(listOf(0, 50, 310, 350), xs)
    }

    @Test
    fun `the center group is centered on the screen`() {
        val xs = placeSlots(listOf(spec(BarGroup.Left, 40), spec(BarGroup.Center, 100), spec(BarGroup.Right, 40)), total, gap, gap)
        assertEquals(150, xs[1])
    }

    @Test
    fun `the anchor, not the group, is what stays centered`() {
        // Indicadores (30) + gap + reloj (80, ancla): el centro del reloj cae en x = 200.
        val xs = placeSlots(listOf(spec(BarGroup.Center, 30), spec(BarGroup.Center, 80, anchor = true)), total, gap, gap)
        assertEquals(200, xs[1] + 40)
        assertEquals(xs[1] - 40, xs[0])
    }

    @Test
    fun `the center slides away from a wide neighbour instead of overlapping it`() {
        val xs = placeSlots(listOf(spec(BarGroup.Left, 190), spec(BarGroup.Center, 100), spec(BarGroup.Right, 30)), total, gap, gap)
        // Centrado estaría en 150, pero la izquierda llega hasta 190: se aparta a 200.
        assertEquals(200, xs[1])
        val right = placeSlots(listOf(spec(BarGroup.Center, 100), spec(BarGroup.Right, 190)), total, gap, gap)
        assertEquals(100, right[0])
    }

    @Test
    fun `without room the center stays next to the left group`() {
        val xs = placeSlots(listOf(spec(BarGroup.Left, 200), spec(BarGroup.Center, 100), spec(BarGroup.Right, 150)), total, gap, gap)
        assertEquals(210, xs[1])
    }

    @Test
    fun `a module that paints nothing takes no space and no gap`() {
        val xs =
            placeSlots(
                listOf(
                    spec(BarGroup.Left, 40),
                    spec(BarGroup.Left, 0),
                    spec(BarGroup.Left, 40),
                    spec(BarGroup.Right, 0),
                    spec(BarGroup.Right, 30),
                ),
                total,
                gap,
                gap,
            )
        assertEquals(listOf(0, 0, 50), xs.take(3))
        assertEquals(370, xs[4])
    }

    @Test
    fun `items of a group touch each other and groups keep their own distance`() {
        // Sin separación entre módulos, pero 20 px mínimos entre grupos.
        val xs =
            placeSlots(
                listOf(spec(BarGroup.Left, 100), spec(BarGroup.Left, 60), spec(BarGroup.Center, 100), spec(BarGroup.Right, 60)),
                total,
                itemGap = 0,
                groupGap = 20,
            )
        assertEquals(listOf(0, 100), xs.take(2))
        // Centrado estaría en 150 (cabe: la izquierda acaba en 160) → se aparta a 180.
        assertEquals(180, xs[2])
        assertEquals(340, xs[3])
    }

    @Test
    fun `an empty bar places nothing`() {
        assertEquals(emptyList<Int>(), placeSlots(emptyList(), total, gap, gap))
        assertEquals(listOf(180), placeSlots(listOf(spec(BarGroup.Center, 40)), total, gap, gap))
    }

    @Test
    fun `the center group steps aside from the camera notch`() {
        // Centro de 100 en [150, 250) y el recorte en [190, 210): pasa a su derecha, pegado al recorte.
        val notch = 190..209
        val xs = placeSlots(listOf(spec(BarGroup.Left, 40), spec(BarGroup.Center, 100), spec(BarGroup.Right, 40)), total, gap, gap, notch)
        assertEquals(210, xs[1])
        // Sin sitio a la derecha, a la izquierda.
        val wide =
            placeSlots(listOf(spec(BarGroup.Left, 40), spec(BarGroup.Center, 100), spec(BarGroup.Right, 120)), total, gap, gap, notch)
        assertEquals(90, wide[1])
        // Lejos del recorte, nada cambia.
        assertEquals(150, placeSlots(listOf(spec(BarGroup.Center, 100)), total, gap, gap, 0..9)[0])
    }
}
