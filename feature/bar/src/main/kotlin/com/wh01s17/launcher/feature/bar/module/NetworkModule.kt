package com.wh01s17.launcher.feature.bar.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.status.NetworkKind
import com.wh01s17.launcher.core.system.status.NetworkState
import com.wh01s17.launcher.feature.bar.R

/**
 * `wh01s17.network`: la red por defecto. Clases: el tipo (`wifi`, `cellular`, `ethernet`, `other`,
 * `none`), `no-internet` (conectado pero sin salida) y `vpn`. Sin conexión se atenúa; sin internet
 * va en amarillo.
 */
internal class NetworkModule : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val network = context.state.network
        val style =
            rememberModuleStyle(
                context.settings,
                classesOf(network),
                moduleDefaults(classColors = mapOf(NO_INTERNET to WhTheme.colors.yellow), dimClasses = setOf(NONE)),
            )
        val kindText =
            stringResource(
                when (network.kind) {
                    NetworkKind.None -> R.string.bar_network_none
                    NetworkKind.Wifi -> R.string.bar_network_wifi
                    NetworkKind.Cellular -> R.string.bar_network_cellular
                    NetworkKind.Ethernet -> R.string.bar_network_ethernet
                    NetworkKind.Other -> R.string.bar_network_other
                },
            )
        val description =
            kindText +
                (if (network.kind != NetworkKind.None && !network.hasInternet) stringResource(R.string.bar_network_no_internet) else "") +
                (if (network.vpn) stringResource(R.string.bar_network_vpn) else "")
        BarItem(style, contentDescription = description) { WhIcon(glyphOf(network.kind)) }
    }

    companion object {
        const val ID = "wh01s17.network"
        const val NONE = "none"
        const val NO_INTERNET = "no-internet"

        fun classesOf(network: NetworkState): List<String> =
            buildList {
                add(network.kind.name.lowercase())
                if (network.kind != NetworkKind.None && !network.hasInternet) add(NO_INTERNET)
                if (network.vpn) add("vpn")
            }

        fun glyphOf(kind: NetworkKind): WhGlyph =
            when (kind) {
                NetworkKind.None -> WhIcons.WifiOff
                NetworkKind.Wifi -> WhIcons.Wifi
                NetworkKind.Cellular -> WhIcons.Cellular
                NetworkKind.Ethernet, NetworkKind.Other -> WhIcons.Ethernet
            }
    }
}
