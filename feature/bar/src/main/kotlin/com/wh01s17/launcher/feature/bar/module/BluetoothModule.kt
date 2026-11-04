package com.wh01s17.launcher.feature.bar.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.status.BluetoothStatus
import com.wh01s17.launcher.feature.bar.R

/**
 * `wh01s17.bluetooth`. Clases: `off`, `on` y `connected` (sólo se sabe con el permiso opt-in, plan.md
 * R8). Apagado se atenúa, conectado va en verde. Sin adaptador no pinta nada.
 */
internal class BluetoothModule : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val status = context.state.bluetooth
        if (status == BluetoothStatus.Unavailable) return
        val style =
            rememberModuleStyle(
                context.settings,
                listOf(classOf(status)),
                moduleDefaults(classColors = mapOf(CONNECTED to WhTheme.colors.accent), dimClasses = setOf(OFF)),
            )
        val description =
            stringResource(
                when (status) {
                    BluetoothStatus.Connected -> R.string.bar_bluetooth_connected
                    BluetoothStatus.On -> R.string.bar_bluetooth_on
                    else -> R.string.bar_bluetooth_off
                },
            )
        BarItem(style, contentDescription = description) { WhIcon(glyphOf(status)) }
    }

    companion object {
        const val ID = "wh01s17.bluetooth"
        const val OFF = "off"
        const val ON = "on"
        const val CONNECTED = "connected"

        fun classOf(status: BluetoothStatus): String =
            when (status) {
                BluetoothStatus.Connected -> CONNECTED
                BluetoothStatus.On -> ON
                else -> OFF
            }

        fun glyphOf(status: BluetoothStatus): WhGlyph =
            when (status) {
                BluetoothStatus.Connected -> WhIcons.BluetoothConnected
                BluetoothStatus.On -> WhIcons.Bluetooth
                else -> WhIcons.BluetoothOff
            }
    }
}
