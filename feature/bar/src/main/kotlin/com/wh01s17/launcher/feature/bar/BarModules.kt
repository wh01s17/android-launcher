package com.wh01s17.launcher.feature.bar

import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.feature.bar.clock.ClockModule
import com.wh01s17.launcher.feature.bar.module.AudioModule
import com.wh01s17.launcher.feature.bar.module.BarModule
import com.wh01s17.launcher.feature.bar.module.BarModuleRegistry
import com.wh01s17.launcher.feature.bar.module.BluetoothModule
import com.wh01s17.launcher.feature.bar.module.IndicatorsModule
import com.wh01s17.launcher.feature.bar.module.MenuModule
import com.wh01s17.launcher.feature.bar.module.NetworkModule
import com.wh01s17.launcher.feature.bar.module.PowerModule
import com.wh01s17.launcher.feature.bar.module.WorkspacesModule

/** Los módulos del MVP (plan.md §5.5): menú, escritorios, reloj, indicadores, energía, red, Bluetooth y audio. */
fun defaultBarModules(clock: ClockSource): List<BarModule> =
    listOf(
        MenuModule(),
        WorkspacesModule(),
        ClockModule(clock),
        IndicatorsModule(),
        PowerModule(),
        NetworkModule(),
        BluetoothModule(),
        AudioModule(),
    )

fun defaultBarRegistry(clock: ClockSource): BarModuleRegistry = BarModuleRegistry(defaultBarModules(clock))
