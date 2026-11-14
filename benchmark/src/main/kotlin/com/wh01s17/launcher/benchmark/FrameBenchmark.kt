package com.wh01s17.launcher.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fotogramas al deslizar escritorios y al abrir el menú (plan.md §7 Hito 11: 120 fps a 120 Hz, es
 * decir, `frameDurationCpuMs` y `frameOverrunMs` por debajo de 8,3 ms).
 */
@RunWith(AndroidJUnit4::class)
class FrameBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun workspacePager() = measureFrames { swipeWorkspaces() }

    @Test
    fun menuOpen() = measureFrames { repeat(MENU_OPENINGS) { openAndCloseMenu() } }

    private fun measureFrames(block: MacrobenchmarkScope.() -> Unit) =
        rule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            iterations = ITERATIONS,
            startupMode = StartupMode.WARM,
            compilationMode = compilation(CompilationMode.Partial(BaselineProfileMode.Require)),
            setupBlock = {
                pressHome()
                startActivityAndWait()
            },
            measureBlock = block,
        )

    private companion object {
        const val ITERATIONS = 5
        const val MENU_OPENINGS = 3
    }
}
