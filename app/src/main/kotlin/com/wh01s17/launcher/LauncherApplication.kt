package com.wh01s17.launcher

import android.app.Application
import com.wh01s17.launcher.core.designsystem.palette.PaletteHost
import com.wh01s17.launcher.core.designsystem.palette.PlayerPalette
import com.wh01s17.launcher.core.designsystem.palette.TidalampPalettes
import com.wh01s17.launcher.di.AppContainer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Corre en dos procesos: el del launcher y el de navegación (`:nav`, el servicio de accesibilidad).
 * Sólo el del launcher arranca la vigilancia de la navegación y los temporizadores (servicio,
 * alarma y avisos del pomodoro y el metrónomo); el otro no necesita nada de aquí.
 */
class LauncherApplication :
    Application(),
    PaletteHost {
    lateinit var container: AppContainer
        private set

    override val palettes: Flow<PlayerPalette>
        get() =
            container.configRepository.state
                .map { TidalampPalettes.byName(it.config?.appearance?.palette) }
                .distinctUntilChanged()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (getProcessName() == packageName) {
            container.navigationWatchdog.start()
            container.timers.start()
            container.processScope.launch { container.configRepository.reload() }
        }
    }
}
