package com.wh01s17.launcher.core.designsystem.screensaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpinGeometryTest {
    private fun lit(frame: IntArray) = frame.count { it > 0 }

    @Test
    fun `rotation stays on the dot grid and changes the silhouette`() {
        val frames = listOf(0.0, Math.PI / 2, Math.PI, Math.PI * 2).map(SpinGeometry::render)
        frames.forEach { frame ->
            assertEquals(SpinGeometry.DOT_WIDTH * SpinGeometry.DOT_HEIGHT, frame.size)
            assertTrue(frame.all { it in 0..SpinGeometry.LEVELS })
            assertTrue(lit(frame) > 0)
            // Fila de arriba y de abajo vacías: la W cabe en la rejilla.
            assertTrue(frame.take(SpinGeometry.DOT_WIDTH).all { it == 0 })
            assertTrue(frame.takeLast(SpinGeometry.DOT_WIDTH).all { it == 0 })
        }
        // De canto se ven muchos menos puntos que de frente.
        assertTrue(lit(frames[1]) < lit(frames[0]) / 2)
        assertTrue(frames[0].contentEquals(frames[3]))
    }

    @Test
    fun `the cycle rests facing forward and spins twice per leg`() {
        assertEquals(0.0, SpinGeometry.yaw(0.0), 0.0)
        assertEquals(0.0, SpinGeometry.yaw(0.5), 0.0)
        assertEquals(0.0, SpinGeometry.yaw(0.99), 0.0)
        assertEquals(4 * Math.PI, SpinGeometry.yaw(0.4599), 0.01)
        assertTrue(SpinGeometry.yaw(0.2) in 0.0..(4 * Math.PI))
    }
}
