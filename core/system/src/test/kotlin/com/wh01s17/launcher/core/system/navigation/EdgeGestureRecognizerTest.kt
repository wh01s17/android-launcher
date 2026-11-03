package com.wh01s17.launcher.core.system.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EdgeGestureRecognizerTest {
    private val config = EdgeGestureConfig(triggerDistancePx = 100f, stillSlopPx = 10f, recentsHoldMillis = 300)

    private fun recognizer(edge: Edge) = EdgeGestureRecognizer(edge, config)

    /** Desliza en línea recta de (x0, y0) a (x1, y1) en [steps] pasos de 10 ms y suelta. */
    private fun EdgeGestureRecognizer.swipe(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        steps: Int = 10,
    ): NavAction? {
        down(x0, y0, 0)
        for (i in 1..steps) move(x0 + (x1 - x0) * i / steps, y0 + (y1 - y0) * i / steps, i * 10L)
        return up(x1, y1, steps * 10L + 10)
    }

    @Test
    fun `swiping inward from either side is back`() {
        assertEquals(NavAction.Back, recognizer(Edge.Left).swipe(0f, 500f, 150f, 520f))
        assertEquals(NavAction.Back, recognizer(Edge.Right).swipe(1000f, 500f, 850f, 480f))
    }

    @Test
    fun `too short, wrong direction or too vertical does nothing`() {
        assertNull(recognizer(Edge.Left).swipe(0f, 500f, 80f, 500f))
        assertNull(recognizer(Edge.Right).swipe(1000f, 500f, 1150f, 500f))
        // Un scroll vertical pegado al borde no es atrás.
        assertNull(recognizer(Edge.Left).swipe(0f, 500f, 110f, 700f))
    }

    @Test
    fun `going back to the edge before lifting cancels`() {
        val r = recognizer(Edge.Left)
        r.down(0f, 500f, 0)
        assertEquals(GestureFeedback.Armed, r.move(150f, 500f, 10))
        assertEquals(GestureFeedback.Disarmed, r.move(40f, 500f, 20))
        assertNull(r.up(40f, 500f, 30))
    }

    @Test
    fun `a quick swipe up is home and allows some tilt`() {
        assertEquals(NavAction.Home, recognizer(Edge.Bottom).swipe(500f, 2000f, 560f, 1850f))
        assertNull(recognizer(Edge.Bottom).swipe(500f, 2000f, 800f, 1900f))
    }

    @Test
    fun `swipe up and hold is recents`() {
        val r = recognizer(Edge.Bottom)
        r.down(500f, 2000f, 0)
        assertEquals(GestureFeedback.Armed, r.move(500f, 1880f, 50))
        // Quieto (dentro de la tolerancia) desde t = 50: el plazo vence a los 350 ms.
        r.move(503f, 1878f, 200)
        assertEquals(350L, r.holdDeadline)
        assertNull(r.checkHold(349))
        assertEquals(GestureFeedback.RecentsArmed, r.checkHold(350))
        assertNull(r.holdDeadline)
        assertEquals(NavAction.Recents, r.up(503f, 1878f, 450))
    }

    @Test
    fun `holding is decided at release too and movement resets the hold`() {
        val r = recognizer(Edge.Bottom)
        r.down(500f, 2000f, 0)
        r.move(500f, 1880f, 50)
        r.move(500f, 1800f, 300) // se movió: el plazo empieza de nuevo
        assertEquals(NavAction.Home, r.up(500f, 1800f, 500))
        r.down(500f, 2000f, 1000)
        r.move(500f, 1880f, 1050)
        assertEquals(NavAction.Recents, r.up(500f, 1880f, 1400))
    }

    @Test
    fun `side edges never ask for recents`() {
        val r = recognizer(Edge.Left)
        r.down(0f, 500f, 0)
        r.move(150f, 500f, 10)
        assertNull(r.holdDeadline)
        assertEquals(NavAction.Back, r.up(150f, 500f, 2000))
    }

    @Test
    fun `cancel and events without down are ignored`() {
        val r = recognizer(Edge.Left)
        assertNull(r.move(150f, 500f, 10))
        assertNull(r.up(150f, 500f, 20))
        r.down(0f, 500f, 0)
        r.move(150f, 500f, 10)
        r.cancel()
        assertNull(r.up(150f, 500f, 20))
    }
}
