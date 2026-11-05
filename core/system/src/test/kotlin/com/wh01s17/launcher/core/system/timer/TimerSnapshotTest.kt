package com.wh01s17.launcher.core.system.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerSnapshotTest {
    @Test
    fun `only what runs is in the snapshot`() {
        val stopped = TimerSnapshot.of(PomodoroState(), MetronomeState())
        assertFalse(stopped.active)
        assertNull(stopped.pomodoro)
        assertNull(stopped.metronome)
        val running = Pomodoro.toggle(PomodoroState(), now = 0)
        assertTrue(TimerSnapshot.of(running, MetronomeState()).active)
        assertEquals(running, TimerSnapshot.of(running, MetronomeState()).pomodoro)
        val playing = MetronomeState(running = true)
        assertEquals(playing.settings, TimerSnapshot.of(PomodoroState(), playing).metronome)
    }

    @Test
    fun `metronome beats do not change the snapshot`() {
        val first = MetronomeState(running = true, beat = 0, ticks = 1)
        val second = first.copy(beat = 1, ticks = 2)
        assertEquals(TimerSnapshot.of(PomodoroState(), first), TimerSnapshot.of(PomodoroState(), second))
    }
}
