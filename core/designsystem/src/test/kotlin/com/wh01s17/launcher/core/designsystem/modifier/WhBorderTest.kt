package com.wh01s17.launcher.core.designsystem.modifier

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WhBorderTest {
    private fun assertOffset(
        expected: Offset,
        actual: Offset,
    ) {
        assertEquals(expected.x, actual.x, 0.01f)
        assertEquals(expected.y, actual.y, 0.01f)
    }

    @Test
    fun `45 degrees on a square goes corner to corner, top left to bottom right`() {
        val (from, to) = gradientLine(Size(100f, 100f), 45f)
        assertOffset(Offset(0f, 0f), from)
        assertOffset(Offset(100f, 100f), to)
    }

    @Test
    fun `45 degrees on a wide box still ends exactly at the opposite corners' projection`() {
        val size = Size(400f, 100f)
        val (from, to) = gradientLine(size, 45f)
        // Las esquinas opuestas quedan justo en los extremos: proyectan en 0 y en la longitud.
        val direction = to - from
        val length = direction.getDistance()
        val unit = Offset(direction.x / length, direction.y / length)

        fun progress(point: Offset) = ((point - from).x * unit.x + (point - from).y * unit.y) / length
        assertEquals(0f, progress(Offset(0f, 0f)), 0.001f)
        assertEquals(1f, progress(Offset(size.width, size.height)), 0.001f)
        // Arriba a la derecha y abajo a la izquierda quedan entre medias, no en un extremo.
        assertEquals(true, progress(Offset(size.width, 0f)) in 0.01f..0.99f)
        assertEquals(true, progress(Offset(0f, size.height)) in 0.01f..0.99f)
    }

    @Test
    fun `zero degrees goes left to right`() {
        val (from, to) = gradientLine(Size(200f, 50f), 0f)
        assertOffset(Offset(0f, 25f), from)
        assertOffset(Offset(200f, 25f), to)
    }

    @Test
    fun `90 degrees goes top to bottom`() {
        val (from, to) = gradientLine(Size(200f, 50f), 90f)
        assertOffset(Offset(100f, 0f), from)
        assertOffset(Offset(100f, 50f), to)
    }

    @Test
    fun `gradient brushes compare by value`() {
        val a = WhGradientBrush(listOf(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), 45f)
        val b = WhGradientBrush(listOf(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), 45f)
        val c = WhGradientBrush(listOf(androidx.compose.ui.graphics.Color.Red, androidx.compose.ui.graphics.Color.Blue), 90f)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }
}
