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
import com.wh01s17.launcher.core.system.media.AudioSpectrum
import com.wh01s17.launcher.core.system.media.MediaSource
import com.wh01s17.launcher.core.system.navigation.NavigationWatchdog
import com.wh01s17.launcher.core.system.navigation.NotificationShade
import com.wh01s17.launcher.core.system.notifications.NotificationRepository
import com.wh01s17.launcher.core.system.oem.xiaomi.AndroidCompatibilityGateway
import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilityGateway
import com.wh01s17.launcher.core.system.status.AudioSource
import com.wh01s17.launcher.core.system.status.BatterySource
import com.wh01s17.launcher.core.system.status.BluetoothSource
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.core.system.status.DoNotDisturbSource
import com.wh01s17.launcher.core.system.status.NetworkSource
import com.wh01s17.launcher.core.system.status.StatusSource
import com.wh01s17.launcher.core.system.status.SystemClockSource
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.feature.bar.BarSources
import com.wh01s17.launcher.feature.bar.WorkspaceBarState
import com.wh01s17.launcher.feature.bar.defaultBarRegistry
import com.wh01s17.launcher.feature.bar.module.BarModuleRegistry
import com.wh01s17.launcher.feature.menu.data.DataStoreUsageRepository
import com.wh01s17.launcher.feature.menu.panel.searchCommands
import com.wh01s17.launcher.feature.widgets.host.WidgetHost
import com.wh01s17.launcher.feature.widgets.native.NativeWidgetSources
import com.wh01s17.launcher.feature.workspaces.data.DeskRepository
import com.wh01s17.launcher.feature.workspaces.data.FileDeskStorage
import com.wh01s17.launcher.timer.TimerCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    val usageRepository by lazy {
        DataStoreUsageRepository.create(
            File(appContext.filesDir, "menu-usage.preferences_pb"),
            CoroutineScope(
                processScope.coroutineContext + Dispatchers.IO,
            ),
        )
    }
    val menuCommands get() = searchCommands { appContext.getString(it) }

    val appActions: AppActions by lazy { LauncherAppActions(appContext, launcherAppsGateway) }

    /** Vigila que nunca queden los botones ocultos sin la navegación propia (plan.md §5.9). */
    val navigationWatchdog: NavigationWatchdog by lazy { NavigationWatchdog(appContext) }

    val compatibilityGateway: CompatibilityGateway by lazy { AndroidCompatibilityGateway(appContext, navigationWatchdog) }

    /**
     * Si en este proceso llegó algún evento con Meta (docs/bindings.md). Lo escribe la pantalla de
     * inicio y lo lee la tabla de `Super + K`; sólo en el hilo principal.
     */
    var superKeyDelivered: Boolean = false

    /** Pomodoro y metrónomo: un estado para el tile, la barra y el servicio (plan.md §7 Hito 7). */
    val pomodoro: PomodoroRepository by lazy {
        PomodoroRepository(File(File(appContext.filesDir, "state"), "pomodoro.properties"), processScope)
    }
    val metronome: MetronomeRepository by lazy { MetronomeRepository(File(File(appContext.filesDir, "state"), "metronome.properties")) }

    /** Servicio en primer plano, alarma del fin de fase y avisos del pomodoro y el metrónomo. */
    val timers: TimerCoordinator by lazy { TimerCoordinator(appContext, pomodoro, metronome, processScope) }

    /** Lo que necesitan los widgets propios de wh01s17. */
    val audioSpectrum: AudioSpectrum by lazy { AudioSpectrum(appContext) }

    /** Si el visualizador tiene el permiso de micrófono; lo actualiza la pantalla de inicio. */
    val audioPermission = MutableStateFlow(false)

    /** Lo pone la pantalla de inicio: lanza el diálogo de permiso de micrófono. */
    var requestAudioPermission: () -> Unit = {}

    /** Sesiones multimedia de otras apps (acceso a notificaciones, opt-in). */
    val mediaSource: MediaSource by lazy { MediaSource(appContext) }

    /** Notificaciones de otras apps: insignias, contador de la barra y panel (Hito 8, opt-in). */
    val notifications: NotificationRepository by lazy { NotificationRepository(appContext) }

    /** El panel de notificaciones está abierto; lo cambia el comando `notifications.panel`. */
    val notificationPanelOpen = MutableStateFlow(false)

    val nativeWidgets: NativeWidgetSources by lazy {
        NativeWidgetSources(
            mediaSource,
            pomodoro,
            metronome,
            clockSource,
            audioSpectrum,
            audioPermission,
        ) { requestAudioPermission() }
    }

    /** Los widgets del sistema en tiles (plan.md §7 Hito 7). */
    val widgetHost: WidgetHost by lazy { WidgetHost(appContext) }

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
            notifications = StatusSource { notifications.state.map { it.summary }.distinctUntilChanged() },
        )
    }

    val barRegistry: BarModuleRegistry by lazy { defaultBarRegistry(clockSource, pomodoro, metronome, mediaSource) }

    val barNotices: BarNotices by lazy { BarNotices(appContext, configRepository.state, compatibilityGateway) }

    /** Los escritorios y sus tiles, guardados en `files/state/workspaces.json` (plan.md §7 Hito 4). */
    val desk: DeskRepository by lazy {
        DeskRepository(FileDeskStorage(File(File(appContext.filesDir, "state"), "workspaces.json")), processScope, Dispatchers.IO)
    }

    /** Lo que la barra necesita de los escritorios: el actual y los que tienen tiles. */
    val workspaceBarState: Flow<WorkspaceBarState> by lazy {
        desk.state
            .map { desk ->
                desk?.let {
                    WorkspaceBarState(
                        focused = it.current,
                        occupied = it.occupied,
                        scratchpadOccupied = !it.scratchpad.isEmpty,
                        scratchpadVisible = it.scratchpadVisible,
                    )
                } ?: WorkspaceBarState()
            }.distinctUntilChanged()
    }

    val notificationShade: NotificationShade by lazy { NotificationShade(appContext, navigationWatchdog) }

    val oneTimeHints: OneTimeHints by lazy { OneTimeHints(appContext) }
}
