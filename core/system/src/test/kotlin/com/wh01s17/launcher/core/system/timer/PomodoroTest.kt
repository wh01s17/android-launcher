package com.wh01s17.launcher.core.system.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PomodoroTest {
    private val minute = PomodoroState.MILLIS_PER_MINUTE
    private val classic = PomodoroState(preset = PomodoroPreset.Classic)

    @Test
    fun `pausing keeps the remaining time and resuming continues from it`() {
        val running = Pomodoro.toggle(classic, now = 0)
        assertTrue(running.running)
        val paused = Pomodoro.toggle(running, now = 10 * minute)
        assertFalse(paused.running)
        assertEquals(15 * minute, paused.remainingAt(99 * minute))
        assertEquals(5 * minute, Pomodoro.toggle(paused, now = 50 * minute).remainingAt(60 * minute))
    }

    @Test
    fun `work goes to short breaks and every fourth session to a long one`() {
        var state = Pomodoro.toggle(classic, now = 0)
        val phases = mutableListOf<PomodoroPhase>()
        var now = 0L
        repeat(8) {
            now += state.remainingAt(now)
            state = Pomodoro.tick(state, now)
            phases += state.phase
        }
        assertEquals(
            listOf(
                PomodoroPhase.ShortBreak,
                PomodoroPhase.Work,
                PomodoroPhase.ShortBreak,
                PomodoroPhase.Work,
                PomodoroPhase.ShortBreak,
                PomodoroPhase.Work,
                PomodoroPhase.LongBreak,
                PomodoroPhase.Work,
            ),
            phases,
        )
        assertTrue("the next phase starts by itself", state.running)
    }

    @Test
    fun `a long sleep advances every finished phase`() {
        val state = Pomodoro.tick(Pomodoro.toggle(classic, now = 0), now = 31 * minute)
        assertEquals(PomodoroPhase.Work, state.phase)
        assertEquals(24 * minute, state.remainingAt(31 * minute))
    }

    @Test
    fun `skip, adjust, reset and presets`() {
        val skipped = Pomodoro.skip(classic, now = 0)
        assertEquals(PomodoroPhase.ShortBreak to false, skipped.phase to skipped.running)
        assertEquals(minute, Pomodoro.adjust(skipped, -10, now = 0).remainingAt(0))
        assertEquals(26 * minute, Pomodoro.adjust(classic, 1, now = 0).remainingAt(0))
        assertEquals(PomodoroState(preset = PomodoroPreset.Classic), Pomodoro.reset(skipped))
        val ultradian = Pomodoro.setPreset(PomodoroPreset.Ultradian)
        assertEquals(90 * minute, ultradian.remainingAt(0))
        val afterTwo = Pomodoro.skip(Pomodoro.skip(Pomodoro.skip(ultradian, 0), 0), 0)
        assertEquals("ultradian has a long break every two sessions", PomodoroPhase.LongBreak, afterTwo.phase)
    }

    @Test
    fun `only a phase that runs out by itself counts as finished`() {
        val running = Pomodoro.toggle(classic, now = 0)
        val ended = Pomodoro.tick(running, now = 25 * minute)
        assertEquals(PomodoroPhase.Work, Pomodoro.finished(running, ended, now = 25 * minute))
        // Saltar, pausar o reiniciar antes de tiempo no avisan.
        assertEquals(null, Pomodoro.finished(running, Pomodoro.skip(running, now = minute), now = minute))
        assertEquals(null, Pomodoro.finished(running, Pomodoro.toggle(running, now = minute), now = minute))
        assertEquals(null, Pomodoro.finished(running, Pomodoro.reset(running), now = minute))
        // Parado no hay nada que terminar.
        assertEquals(null, Pomodoro.finished(classic, Pomodoro.skip(classic, now = 0), now = 99 * minute))
    }
}
