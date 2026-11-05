package com.wh01s17.launcher.feature.bar

import com.wh01s17.launcher.core.system.timer.Pomodoro
import com.wh01s17.launcher.core.system.timer.PomodoroPhase
import com.wh01s17.launcher.core.system.timer.PomodoroPreset
import com.wh01s17.launcher.core.system.timer.PomodoroState
import com.wh01s17.launcher.feature.bar.timer.PomodoroBarModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PomodoroBarModelTest {
    private val minute = PomodoroState.MILLIS_PER_MINUTE

    @Test
    fun `classes follow pomodoro sh`() {
        val idle = PomodoroState()
        assertEquals("work-paused", PomodoroBarModel.classes(idle))
        assertEquals("work-running", PomodoroBarModel.classes(Pomodoro.toggle(idle, now = 0)))
        assertEquals("short-break-paused", PomodoroBarModel.classes(idle.copy(phase = PomodoroPhase.ShortBreak)))
        assertEquals("long-break-running", PomodoroBarModel.classes(idle.copy(phase = PomodoroPhase.LongBreak, running = true)))
    }

    @Test
    fun `only an unstarted pomodoro is idle`() {
        val idle = PomodoroState(preset = PomodoroPreset.Classic)
        assertTrue(PomodoroBarModel.idle(idle, now = 0))
        val running = Pomodoro.toggle(idle, now = 0)
        assertFalse(PomodoroBarModel.idle(running, now = 0))
        // En pausa a mitad de fase hay que ver lo que falta.
        assertFalse(PomodoroBarModel.idle(Pomodoro.toggle(running, now = minute), now = 2 * minute))
        // Un descanso en pausa, aunque entero, dice en qué fase se quedó.
        assertFalse(PomodoroBarModel.idle(Pomodoro.skip(idle, now = 0), now = 0))
        assertTrue(PomodoroBarModel.idle(Pomodoro.reset(running), now = 0))
    }

    @Test
    fun `clock rounds down and shows hours only when needed`() {
        assertEquals("25:00", PomodoroBarModel.clock(25 * minute))
        assertEquals("0:59", PomodoroBarModel.clock(59_999))
        assertEquals("1:30:00", PomodoroBarModel.clock(90 * minute))
        assertEquals("0:00", PomodoroBarModel.clock(0))
    }
}
