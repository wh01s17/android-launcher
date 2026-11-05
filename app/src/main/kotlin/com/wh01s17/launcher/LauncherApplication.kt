package com.wh01s17.launcher

import android.app.Application
import com.wh01s17.launcher.di.AppContainer
import kotlinx.coroutines.launch

/**
 * Corre en dos procesos: el del launcher y el de navegación (`:nav`, el servicio de accesibilidad).
 * Sólo el del launcher arranca la vigilancia de la navegación y los temporizadores (servicio,
 * alarma y avisos del pomodoro y el metrónomo); el otro no necesita nada de aquí.
 */
class LauncherApplication : Application() {
    lateinit var container: AppContainer
        private set

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
