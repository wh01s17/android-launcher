package com.wh01s17.launcher.feature.bar

import com.wh01s17.launcher.core.system.media.MediaSource
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.feature.bar.clock.ClockModule
import com.wh01s17.launcher.feature.bar.module.AudioModule
import com.wh01s17.launcher.feature.bar.module.BarModule
import com.wh01s17.launcher.feature.bar.module.BarModuleRegistry
import com.wh01s17.launcher.feature.bar.module.BluetoothModule
import com.wh01s17.launcher.feature.bar.module.IndicatorsModule
import com.wh01s17.launcher.feature.bar.module.MediaModule
import com.wh01s17.launcher.feature.bar.module.MenuModule
import com.wh01s17.launcher.feature.bar.module.NetworkModule
import com.wh01s17.launcher.feature.bar.module.PowerModule
import com.wh01s17.launcher.feature.bar.module.WorkspacesModule
import com.wh01s17.launcher.feature.bar.timer.MetronomeModule
import com.wh01s17.launcher.feature.bar.timer.PomodoroModule

/**
 * Los módulos de la barra (plan.md §5.5): los del MVP (menú, escritorios, reloj, indicadores,
 * energía, red, Bluetooth y audio), el pomodoro y el metrónomo del Hito 7, sobre los mismos
 * repositorios que sus tiles, y `media` del Hito 8.
 */
fun defaultBarModules(
    clock: ClockSource,
    pomodoro: PomodoroRepository,
    metronome: MetronomeRepository,
    media: MediaSource,
): List<BarModule> =
    listOf(
        MenuModule(),
        WorkspacesModule(),
        ClockModule(clock),
        IndicatorsModule(),
        PowerModule(),
        NetworkModule(),
        BluetoothModule(),
        AudioModule(),
        PomodoroModule(pomodoro),
        MetronomeModule(metronome),
        MediaModule(media),
    )

fun defaultBarRegistry(
    clock: ClockSource,
    pomodoro: PomodoroRepository,
    metronome: MetronomeRepository,
    media: MediaSource,
): BarModuleRegistry = BarModuleRegistry(defaultBarModules(clock, pomodoro, metronome, media))
