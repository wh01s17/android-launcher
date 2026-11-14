package com.wh01s17.launcher.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Arranque en frío (plan.md §7 Hito 11: < 400 ms en el Redmi). Sin compilar frente a con el Baseline
 * Profile, para ver lo que aporta.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartWithoutProfile() = coldStart(CompilationMode.None())

    @Test
    fun coldStartWithBaselineProfile() = coldStart(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun coldStart(mode: CompilationMode) =
        rule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            iterations = ITERATIONS,
            startupMode = StartupMode.COLD,
            compilationMode = compilation(mode),
        ) {
            pressHome()
            startActivityAndWait()
        }

    private companion object {
        const val ITERATIONS = 10
    }
}
