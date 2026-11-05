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

    @Test
    fun `the low end is a smooth slope, not many bands repeating one bin`() {
        // Graves que bajan de forma suave del bin 2 al 40, como en una canción real.
        val fft = ByteArray(1024)
        for (k in 2..40) fft[2 * k] = (120 - (k - 2) * 3).toByte()
        val low = SpectrumMath.bands(fft, 48).take(6)
        assertTrue("the first bands are not a flat line: $low", low.distinct().size == low.size)
        assertTrue("loud bass is not clipped to the top: $low", low.all { it < 0.95f })
    }

    @Test
    fun `the DC and the first bin do not light anything`() {
        val fft =
            ByteArray(1024).also {
                it[0] = 127
                it[2] = 127
            }
        assertTrue(SpectrumMath.bands(fft, 48).all { it == 0f })
    }
}
