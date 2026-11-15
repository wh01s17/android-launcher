package com.wh01s17.launcher.feature.bar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarLayoutTest {
    private fun spec(
        group: BarGroup,
        width: Int,
        anchor: Boolean = false,
        collapsible: Boolean = false,
    ) = SlotSpec(group, width, anchor, collapsible)

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

    @Test
    fun `the required width counts every group and the gaps between them`() {
        val slots = listOf(spec(BarGroup.Left, 100), spec(BarGroup.Left, 50), spec(BarGroup.Right, 80), spec(BarGroup.Center, 0))
        assertEquals(240, requiredWidth(slots, itemGap = 0, groupGap = gap))
        assertEquals(0, requiredWidth(emptyList(), 0, gap))
    }

    @Test
    fun `idle modules hide from the right only until the rest fits`() {
        // Escritorio 6 con el pomodoro en marcha: 230 + 10 + 200 = 440 > 400.
        val slots =
            listOf(
                spec(BarGroup.Left, 230),
                spec(BarGroup.Right, 30),
                spec(BarGroup.Right, 30, collapsible = true),
                spec(BarGroup.Right, 30, collapsible = true),
                spec(BarGroup.Right, 110),
            )
        assertEquals(setOf(3), collapsedSlots(slots, 420, 0, gap))
        assertEquals(setOf(2, 3), collapsedSlots(slots, total, 0, gap))
        assertEquals(emptySet<Int>(), collapsedSlots(slots, 440, 0, gap))
    }

    @Test
    fun `without idle modules nothing hides even if it does not fit`() {
        val slots = listOf(spec(BarGroup.Left, 300), spec(BarGroup.Right, 200))
        assertEquals(emptySet<Int>(), collapsedSlots(slots, total, 0, gap))
    }

    @Test
    fun `the bar compacts when it overflows and returns once the saving fits again`() {
        val compaction = BarCompaction()
        compaction.update(required = 440, available = total)
        assertTrue(compaction.compact)
        // Ya compacta: 380 ahorra 60, y 380 + 60 no cabe.
        compaction.composedCompact = true
        compaction.update(required = 380, available = total)
        assertTrue(compaction.compact)
        // El pomodoro se para y su texto desaparece: 330 + 60 cabe con el relleno normal.
        compaction.update(required = 330, available = total)
        assertFalse(compaction.compact)
        compaction.composedCompact = false
        compaction.update(required = 390, available = total)
        assertFalse(compaction.compact)
    }

    @Test
    fun `as a last resort the right group hides from the right, never the left group or the anchor`() {
        val slots =
            listOf(
                spec(BarGroup.Left, 200),
                spec(BarGroup.Center, 60, anchor = true),
                spec(BarGroup.Right, 50),
                spec(BarGroup.Right, 50),
            )
        // 200 + 60 + 50 + 50 + 3 huecos de grupo (30) = 390 > 340: sobran 50, se va el de más a la derecha.
        assertEquals(setOf(3), overflowSlots(slots, 340, 0, gap))
        // Sin sitio ni para el ancla con la izquierda, se va todo el grupo derecho, pero no el ancla.
        assertEquals(setOf(2, 3), overflowSlots(slots, 200, 0, gap))
        assertEquals(emptySet<Int>(), overflowSlots(slots, 400, 0, gap))
    }

    @Test
    fun `the bar turns tight only once compact and without idle modules it still overflows`() {
        val compaction = BarCompaction()
        compaction.update(required = 500, available = 400)
        assertEquals(true, compaction.compact)
        assertEquals(false, compaction.tight)
        // Ya compacta: sin los módulos en reposo mide 420, aún no cabe.
        compaction.composedCompact = true
        compaction.update(required = 450, available = 400, visible = 420)
        assertEquals(true, compaction.tight)
        // Estrecha: mide 380 y apunta que ahorró 40; con 370 no vuelve (370 + 40 > 400)…
        compaction.composedTight = true
        compaction.update(required = 410, available = 400, visible = 380)
        assertEquals(true, compaction.tight)
        compaction.update(required = 400, available = 400, visible = 370)
        assertEquals(true, compaction.tight)
        // …y con 350 sí (350 + 40 <= 400), pero sigue compacta: medía con los textos cortos.
        compaction.update(required = 380, available = 400, visible = 350)
        assertEquals(false, compaction.tight)
        assertEquals(true, compaction.compact)
    }

    @Test
    fun `while tight the bar never leaves compact, so it cannot loop`() {
        val compaction = BarCompaction()
        compaction.update(required = 500, available = 400)
        compaction.composedCompact = true
        compaction.update(required = 450, available = 400, visible = 420)
        compaction.composedTight = true
        // Con los textos cortos mide muy poco: aun así no sale de compacto mientras esté estrecha.
        compaction.update(required = 200, available = 400, visible = 200)
        assertEquals(true, compaction.compact)
    }
}
