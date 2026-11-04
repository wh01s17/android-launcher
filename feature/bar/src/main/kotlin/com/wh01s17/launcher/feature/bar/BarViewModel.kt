package com.wh01s17.launcher.feature.bar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.LauncherCommandBus
import com.wh01s17.launcher.core.system.status.AudioState
import com.wh01s17.launcher.core.system.status.BatteryState
import com.wh01s17.launcher.core.system.status.BluetoothStatus
import com.wh01s17.launcher.core.system.status.DoNotDisturbState
import com.wh01s17.launcher.core.system.status.NetworkState
import com.wh01s17.launcher.core.system.status.StatusSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Las fuentes de estado del sistema de la barra, agrupadas para pasarlas de una vez. */
class BarSources(
    val battery: StatusSource<BatteryState>,
    val network: StatusSource<NetworkState>,
    val bluetooth: StatusSource<BluetoothStatus>,
    val audio: StatusSource<AudioState>,
    val doNotDisturb: StatusSource<DoNotDisturbState>,
)

/**
 * Estado de la barra: junta las fuentes del sistema, los workspaces y los avisos en un único
 * [BarState]. Las fuentes sólo escuchan al sistema mientras la UI recoge [state]; con el launcher
 * en segundo plano (`collectAsStateWithLifecycle`) se sueltan tras [STOP_TIMEOUT_MILLIS].
 */
class BarViewModel(
    sources: BarSources,
    workspaces: Flow<WorkspaceBarState>,
    notices: Flow<List<BarNotice>>,
    private val commands: LauncherCommandBus,
) : ViewModel() {
    val state: StateFlow<BarState> =
        combine(system(sources), workspaces, notices) { system, workspaces, notices ->
            system.copy(workspaces = workspaces, notices = notices)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), BarState())

    fun send(command: LauncherCommand) = commands.send(command)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Las cinco fuentes del sistema en un [BarState] sin workspaces ni avisos. */
        fun system(sources: BarSources): Flow<BarState> =
            combine(
                sources.battery.observe(),
                sources.network.observe(),
                sources.bluetooth.observe(),
                sources.audio.observe(),
                sources.doNotDisturb.observe(),
            ) { battery, network, bluetooth, audio, doNotDisturb ->
                BarState(battery = battery, network = network, bluetooth = bluetooth, audio = audio, doNotDisturb = doNotDisturb)
            }
    }
}
