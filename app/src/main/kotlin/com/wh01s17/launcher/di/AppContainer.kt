package com.wh01s17.launcher.di

import android.content.Context
import com.wh01s17.launcher.bar.BarNotices
import com.wh01s17.launcher.bar.OneTimeHints
import com.wh01s17.launcher.core.apps.AppRepository
import com.wh01s17.launcher.core.apps.LauncherAppRepository
import com.wh01s17.launcher.core.apps.actions.AppActions
import com.wh01s17.launcher.core.apps.actions.LauncherAppActions
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.apps.icons.LauncherIconLoader
import com.wh01s17.launcher.core.apps.source.AndroidLauncherAppsGateway
import com.wh01s17.launcher.core.config.AndroidConfigSource
import com.wh01s17.launcher.core.config.ConfigRepository
import com.wh01s17.launcher.core.input.DefaultLauncherCommandBus
import com.wh01s17.launcher.core.input.LauncherCommandBus
import com.wh01s17.launcher.core.system.navigation.NavigationWatchdog
import com.wh01s17.launcher.core.system.navigation.NotificationShade
import com.wh01s17.launcher.core.system.oem.xiaomi.AndroidCompatibilityGateway
import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilityGateway
import com.wh01s17.launcher.core.system.status.AudioSource
import com.wh01s17.launcher.core.system.status.BatterySource
import com.wh01s17.launcher.core.system.status.BluetoothSource
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.core.system.status.DoNotDisturbSource
import com.wh01s17.launcher.core.system.status.NetworkSource
import com.wh01s17.launcher.core.system.status.SystemClockSource
import com.wh01s17.launcher.feature.bar.BarSources
import com.wh01s17.launcher.feature.bar.WorkspaceBarState
import com.wh01s17.launcher.feature.bar.defaultBarRegistry
import com.wh01s17.launcher.feature.bar.module.BarModuleRegistry
import com.wh01s17.launcher.feature.workspaces.data.DeskRepository
import com.wh01s17.launcher.feature.workspaces.data.FileDeskStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File

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

    /** Todos los comandos del launcher pasan por aquí (plan.md §5.3). */
    val commandBus: LauncherCommandBus = DefaultLauncherCommandBus()

    val configRepository: ConfigRepository by lazy { ConfigRepository(AndroidConfigSource(appContext), Dispatchers.IO) }

    val clockSource: ClockSource by lazy { SystemClockSource(appContext) }

    val barSources: BarSources by lazy {
        BarSources(
            battery = BatterySource(appContext),
            network = NetworkSource(appContext),
            bluetooth = BluetoothSource(appContext),
            audio = AudioSource(appContext),
            doNotDisturb = DoNotDisturbSource(appContext),
        )
    }

    val barRegistry: BarModuleRegistry by lazy { defaultBarRegistry(clockSource) }

    val barNotices: BarNotices by lazy { BarNotices(appContext, configRepository.state, compatibilityGateway) }

    /** Los escritorios y sus tiles, guardados en `files/state/workspaces.json` (plan.md §7 Hito 4). */
    val desk: DeskRepository by lazy {
        DeskRepository(FileDeskStorage(File(File(appContext.filesDir, "state"), "workspaces.json")), processScope, Dispatchers.IO)
    }

    /** Lo que la barra necesita de los escritorios: el actual y los que tienen tiles. */
    val workspaceBarState: Flow<WorkspaceBarState> by lazy {
        desk.state
            .map { desk -> desk?.let { WorkspaceBarState(focused = it.current, occupied = it.occupied) } ?: WorkspaceBarState() }
            .distinctUntilChanged()
    }

    val notificationShade: NotificationShade by lazy { NotificationShade(appContext, navigationWatchdog) }

    val oneTimeHints: OneTimeHints by lazy { OneTimeHints(appContext) }
}
