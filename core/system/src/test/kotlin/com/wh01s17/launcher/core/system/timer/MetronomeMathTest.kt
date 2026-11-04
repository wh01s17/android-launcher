package com.wh01s17.launcher.core.system.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MetronomeMathTest {
    private val rate = 48_000

    @Test
    fun `the downbeat is accented and subdivisions get the soft voice`() {
        val settings = MetronomeSettings(subdivision = Subdivision.Eighth)
        assertEquals(listOf(ClickVoice.Accent, ClickVoice.Sub), MetronomeMath.voices(settings, beat = 0))
        assertEquals(listOf(ClickVoice.Beat, ClickVoice.Sub), MetronomeMath.voices(settings, beat = 2))
        assertEquals(listOf(ClickVoice.Beat), MetronomeMath.voices(MetronomeSettings(accent = false), beat = 0))
    }

    @Test
    fun `a beat lasts exactly sixty over bpm and clicks start at their offsets`() {
        val beat = MetronomeMath.renderBeat(MetronomeSettings(bpm = 120, subdivision = Subdivision.Eighth), 0, rate)
        assertEquals(24_000, beat.size)
        assertTrue("silence before the second pulse", beat.slice(11_000 until 12_000).all { it.toInt() == 0 })
        assertTrue("second pulse sounds", beat.slice(12_000 until 12_200).any { abs(it.toInt()) > 100 })
    }

    @Test
    fun `tempo names, tap tempo and limits`() {
        assertEquals("Andante", MetronomeMath.tempoName(100))
        assertEquals("Prestissimo", MetronomeMath.tempoName(240))
        assertEquals(120, MetronomeMath.tapTempo(listOf(0, 500, 1000, 1500)))
        assertEquals("a long gap starts over", 60, MetronomeMath.tapTempo(listOf(0, 500, 5000, 6000)))
        assertNull(MetronomeMath.tapTempo(listOf(0)))
        assertEquals(
            MetronomeSettings(bpm = 240, beats = 16, volume = 1f),
            MetronomeSettings(bpm = 999, beats = 40, volume = 3f).normalized(),
        )
    }
}
