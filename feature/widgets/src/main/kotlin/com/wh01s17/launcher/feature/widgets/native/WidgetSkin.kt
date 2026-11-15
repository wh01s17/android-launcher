package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wh01s17.launcher.core.designsystem.widgets.WidgetTheme
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.feature.widgets.native.hack.HackSkin

/**
 * Cómo pinta una temática ([WidgetTheme], se elige en Estilo) cada widget propio. Todas reciben los
 * mismos datos y las mismas acciones: sólo cambia el aspecto. Una temática nueva implementa esta
 * interfaz y se añade a [skinOf] (el `when` es exhaustivo: sin su piel no compila).
 */
internal interface WidgetSkin {
    @Composable
    fun Tidal(
        sources: NativeWidgetSources,
        interactive: Boolean,
        modifier: Modifier,
    )

    @Composable
    fun Pomodoro(
        pomodoro: PomodoroRepository,
        interactive: Boolean,
        modifier: Modifier,
    )

    @Composable
    fun Metronome(
        metronome: MetronomeRepository,
        interactive: Boolean,
        modifier: Modifier,
    )

    @Composable
    fun Clock(
        clock: ClockSource,
        modifier: Modifier,
    )

    @Composable
    fun Usage(modifier: Modifier)
}

internal fun skinOf(theme: WidgetTheme): WidgetSkin =
    when (theme) {
        WidgetTheme.Nova -> NovaSkin
        WidgetTheme.Hack -> HackSkin
    }

/** El tema *nova* de tidalamp: los widgets de siempre. */
private object NovaSkin : WidgetSkin {
    @Composable
    override fun Tidal(
        sources: NativeWidgetSources,
        interactive: Boolean,
        modifier: Modifier,
    ) = TidalWidget(sources, interactive, modifier)

    @Composable
    override fun Pomodoro(
        pomodoro: PomodoroRepository,
        interactive: Boolean,
        modifier: Modifier,
    ) = PomodoroWidget(pomodoro, interactive, modifier)

    @Composable
    override fun Metronome(
        metronome: MetronomeRepository,
        interactive: Boolean,
        modifier: Modifier,
    ) = MetronomeWidget(metronome, interactive, modifier)

    @Composable
    override fun Clock(
        clock: ClockSource,
        modifier: Modifier,
    ) = ClockWidget(clock, modifier)

    @Composable
    override fun Usage(modifier: Modifier) = UsageWidget(modifier)
}
