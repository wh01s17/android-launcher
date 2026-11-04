package com.wh01s17.launcher.core.system.timer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val minute = PomodoroState.MILLIS_PER_MINUTE

    @Test
    fun `the phase advances by itself and the state survives a new process`() =
        runTest {
            val file = File(folder.root, "pomodoro.properties")
            val clock = { testScheduler.currentTime }
            val repository = PomodoroRepository(file, backgroundScope, clock)
            repository.setPreset(PomodoroPreset.Classic)
            repository.toggle()
            advanceTimeBy(25 * minute + 1)
            runCurrent()
            assertEquals(PomodoroPhase.ShortBreak, repository.state.value.phase)
            val reopened = PomodoroRepository(file, backgroundScope, clock)
            assertEquals(repository.state.value, reopened.state.value)
            assertEquals(5 * minute - 1, reopened.state.value.remainingAt(clock()))
        }
}
