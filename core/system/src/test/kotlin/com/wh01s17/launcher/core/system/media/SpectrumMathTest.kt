package com.wh01s17.launcher.core.system.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumMathTest {
    @Test
    fun `silence is flat and a tone lights its band only`() {
        val silence = ByteArray(1024)
        assertTrue(SpectrumMath.bands(silence, 16).all { it == 0f })
        val tone = ByteArray(1024).also { it[2 * 200] = 127 }
        val bands = SpectrumMath.bands(tone, 16)
        assertEquals("one band holds the tone", 1, bands.count { it > 0.5f })
        assertTrue("the tone is in the upper half", bands.indexOfFirst { it > 0.5f } >= 8)
    }
}
