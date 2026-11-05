package com.wh01s17.launcher.feature.bar

import androidx.compose.runtime.Immutable
import com.wh01s17.launcher.core.system.status.AudioState
import com.wh01s17.launcher.core.system.status.BatteryState
import com.wh01s17.launcher.core.system.status.BluetoothStatus
import com.wh01s17.launcher.core.system.status.DoNotDisturbState
import com.wh01s17.launcher.core.system.status.NetworkState

/** Los workspaces tal como los ve la barra: el activo, los que tienen tiles y cuántos se ven siempre. */
@Immutable
data class WorkspaceBarState(
    val focused: Int = 1,
    /** Workspaces con al menos un tile. */
    val occupied: Set<Int> = emptySet(),
    /** Escritorios que se muestran siempre (`persistent`, como Hyprland): 1..[persistent]. */
    val persistent: Int = DEFAULT_PERSISTENT,
    /** El scratchpad tiene tiles / se ve encima: la barra muestra su «S» si alguna es cierta. */
    val scratchpadOccupied: Boolean = false,
    val scratchpadVisible: Boolean = false,
) {
    /** Persistentes más los ocupados y el activo, ordenados; como `workspaceIds()` del shell. */
    val ids: List<Int>
        get() = ((1..persistent) + occupied + focused).filter { it in FIRST..LAST }.distinct().sorted()

    companion object {
        const val FIRST = 1
        const val LAST = 10
        const val DEFAULT_PERSISTENT = 5

        /** El escritorio 10 se llama «0», como la tecla `Super + 0`. */
        fun label(id: Int) = if (id == LAST) "0" else id.toString()
    }
}

enum class NoticeSeverity { Warning, Error }

/**
 * Aviso en la barra (plan.md §5.9 y §6.2): una config que no se pudo aplicar (`error`, rojo) o la
 * capa 2 de Xiaomi que se rindió (`warning`, amarillo).
 */
@Immutable
data class BarNotice(
    val id: String,
    val text: String,
    val severity: NoticeSeverity,
)

/** Todo lo que pintan los módulos de la barra. */
@Immutable
data class BarState(
    val battery: BatteryState = BatteryState.Unknown,
    val network: NetworkState = NetworkState.Offline,
    val bluetooth: BluetoothStatus = BluetoothStatus.Unavailable,
    val audio: AudioState = AudioState(volumePercent = 0, muted = true),
    val doNotDisturb: DoNotDisturbState = DoNotDisturbState(active = false),
    val workspaces: WorkspaceBarState = WorkspaceBarState(),
    val notices: List<BarNotice> = emptyList(),
)
