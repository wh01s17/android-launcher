package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.feature.widgets.native.NativeWidgetSources
import com.wh01s17.launcher.feature.widgets.native.WidgetSkin

/** La temática *hack* (`WidgetTheme.Hack`): los mismos widgets como salida de una terminal ([HackFrame]). */
internal object HackSkin : WidgetSkin {
    @Composable
    override fun Tidal(
        sources: NativeWidgetSources,
        interactive: Boolean,
        modifier: Modifier,
    ) = HackMedia(sources, interactive, modifier)

    @Composable
    override fun Pomodoro(
        pomodoro: PomodoroRepository,
        interactive: Boolean,
        modifier: Modifier,
    ) = HackPomodoro(pomodoro, interactive, modifier)

    @Composable
    override fun Metronome(
        metronome: MetronomeRepository,
        interactive: Boolean,
        modifier: Modifier,
    ) = HackMetronome(metronome, interactive, modifier)

    @Composable
    override fun Clock(
        clock: ClockSource,
        modifier: Modifier,
    ) = HackClock(clock, modifier)

    @Composable
    override fun Usage(modifier: Modifier) = HackUsage(modifier)
}
