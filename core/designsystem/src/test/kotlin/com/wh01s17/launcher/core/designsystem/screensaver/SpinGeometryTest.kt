package com.wh01s17.launcher.core.designsystem.screensaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpinGeometryTest {
    @Test
    fun `rotation stays on the grid and changes the silhouette`() {
        val frames = listOf(0.0, Math.PI / 2, Math.PI, Math.PI * 2).map(SpinGeometry::render)
        frames.forEach { frame ->
            assertEquals(20, frame.size)
            assertTrue(frame.all { it.length == 40 })
            assertTrue(frame.any { it.isNotBlank() })
            assertTrue(frame.first().isBlank())
            assertTrue(frame.last().isBlank())
        }
        assertNotEquals(frames[0], frames[1])
        assertEquals(frames[0], frames[3])
    }
}
