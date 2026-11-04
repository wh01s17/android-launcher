package com.wh01s17.launcher.core.system.status

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.Flow

/** Estado del Bluetooth (módulo `wh01s17.bluetooth`). */
enum class BluetoothStatus {
    /** Sin adaptador. */
    Unavailable,
    Off,
    On,

    /** Encendido y con un dispositivo de audio conectado; sólo se sabe con el permiso. */
    Connected,
}

/**
 * Sin `BLUETOOTH_CONNECT` (API 31+) sólo se sabe si está encendido; saber si hay algo conectado
 * exige ese permiso, que se ofrece como opt-in y no se pide al arrancar (plan.md R8, §5.8). El launcher
 * funciona igual sin él.
 */
class BluetoothSource(
    private val context: Context,
) : StatusSource<BluetoothStatus> {
    private val adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter

    /** `true` si se puede preguntar por dispositivos conectados. */
    val canSeeConnections: Boolean
        get() =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    // Las conexiones se escuchan siempre: sin el permiso Android no entrega la difusión, y si el
    // usuario lo concede con la barra en pantalla empieza a llegar sin volver a registrar nada.
    override fun observe(): Flow<BluetoothStatus> =
        context.broadcastState(intentFilterOf(BluetoothAdapter.ACTION_STATE_CHANGED, BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
            current()
        }

    // Los dos métodos que exigen BLUETOOTH_CONNECT sólo se llaman si `canSeeConnections`.
    @SuppressLint("MissingPermission")
    private fun current(): BluetoothStatus =
        when {
            adapter == null -> BluetoothStatus.Unavailable
            !adapter.isEnabled -> BluetoothStatus.Off
            canSeeConnections && anyAudioConnected(adapter) -> BluetoothStatus.Connected
            else -> BluetoothStatus.On
        }

    @SuppressLint("MissingPermission")
    private fun anyAudioConnected(adapter: BluetoothAdapter): Boolean =
        try {
            listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET).any {
                adapter.getProfileConnectionState(it) == BluetoothAdapter.STATE_CONNECTED
            }
        } catch (ignored: SecurityException) {
            false // el permiso se retiró mientras se escuchaba
        }
}
