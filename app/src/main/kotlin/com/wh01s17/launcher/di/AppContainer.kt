package com.wh01s17.launcher.di

import android.content.Context
import com.wh01s17.launcher.core.apps.AppRepository
import com.wh01s17.launcher.core.apps.LauncherAppRepository
import com.wh01s17.launcher.core.apps.actions.AppActions
import com.wh01s17.launcher.core.apps.actions.LauncherAppActions
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.apps.icons.LauncherIconLoader
import com.wh01s17.launcher.core.apps.source.AndroidLauncherAppsGateway
import com.wh01s17.launcher.core.system.navigation.NavigationWatchdog
import com.wh01s17.launcher.core.system.oem.xiaomi.AndroidCompatibilityGateway
import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilityGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Dependencias de la app, creadas a mano (plan.md §5.1: sin Hilt). Vive lo mismo que el proceso;
 * cada dependencia se crea la primera vez que se usa para no retrasar el arranque.
 */
class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    /** Ámbito del proceso: los repositorios siguen vivos mientras el launcher lo esté. */
    val processScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val launcherAppsGateway: AndroidLauncherAppsGateway by lazy { AndroidLauncherAppsGateway(appContext) }

    val appRepository: AppRepository by lazy {
        LauncherAppRepository(launcherAppsGateway, processScope, Dispatchers.IO)
    }

    val iconLoader: AppIconLoader by lazy { LauncherIconLoader(appContext, launcherAppsGateway, Dispatchers.IO) }

    val appActions: AppActions by lazy { LauncherAppActions(appContext, launcherAppsGateway) }

    /** Vigila que nunca queden los botones ocultos sin la navegación propia (plan.md §5.9). */
    val navigationWatchdog: NavigationWatchdog by lazy { NavigationWatchdog(appContext) }

    val compatibilityGateway: CompatibilityGateway by lazy { AndroidCompatibilityGateway(appContext, navigationWatchdog) }
}
