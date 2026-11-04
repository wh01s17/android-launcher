package com.wh01s17.launcher.core.system.status

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkKind { None, Wifi, Cellular, Ethernet, Other }

/**
 * Red por defecto (módulo `wh01s17.network`). [hasInternet] es `false` si Android no pudo
 * comprobar que sale a internet (p. ej. un portal cautivo o un Wi-Fi sin salida).
 */
data class NetworkState(
    val kind: NetworkKind,
    val hasInternet: Boolean,
    val vpn: Boolean,
) {
    companion object {
        val Offline = NetworkState(NetworkKind.None, hasInternet = false, vpn = false)

        /** Prioridad si una red declara varios transportes: Ethernet, Wi-Fi, móvil. */
        fun kindOf(
            wifi: Boolean,
            cellular: Boolean,
            ethernet: Boolean,
        ): NetworkKind =
            when {
                ethernet -> NetworkKind.Ethernet
                wifi -> NetworkKind.Wifi
                cellular -> NetworkKind.Cellular
                else -> NetworkKind.Other
            }
    }
}

/** [NetworkState] con `registerDefaultNetworkCallback` (permiso normal `ACCESS_NETWORK_STATE`). */
class NetworkSource(
    context: Context,
) : StatusSource<NetworkState> {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    override fun observe(): Flow<NetworkState> =
        callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) {
                        trySend(capabilities.toState())
                    }

                    override fun onLost(network: Network) {
                        trySend(NetworkState.Offline)
                    }
                }
            trySend(NetworkState.Offline)
            connectivity.registerDefaultNetworkCallback(callback)
            awaitClose { connectivity.unregisterNetworkCallback(callback) }
        }.conflate().distinctUntilChanged()

    private fun NetworkCapabilities.toState() =
        NetworkState(
            kind =
                NetworkState.kindOf(
                    wifi = hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                    cellular = hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
                    ethernet = hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
                ),
            hasInternet = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            vpn = hasTransport(NetworkCapabilities.TRANSPORT_VPN),
        )
}
