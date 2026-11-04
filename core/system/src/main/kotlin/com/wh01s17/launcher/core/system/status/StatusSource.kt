package com.wh01s17.launcher.core.system.status

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Una fuente de estado del sistema (batería, red…) como `Flow` frío (plan.md §7, Hito 3): sólo
 * escucha al sistema mientras alguien recoge, así que con la pantalla apagada o el launcher en
 * segundo plano (`collectAsStateWithLifecycle`) no consume nada. Emite el valor actual al empezar y
 * sólo cuando cambia.
 */
fun interface StatusSource<T> {
    fun observe(): Flow<T>
}

/**
 * `Flow` de [read], que se vuelve a evaluar al recibir cualquiera de las acciones de [filter]. [read]
 * recibe el `Intent` recién llegado, o el *sticky* actual al registrar (si lo hay).
 */
internal fun <T> Context.broadcastState(
    filter: IntentFilter,
    read: (Intent?) -> T,
): Flow<T> =
    callbackFlow {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    trySend(read(intent))
                }
            }
        // Son difusiones del sistema: el receptor no necesita estar exportado.
        val sticky = ContextCompat.registerReceiver(this@broadcastState, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(read(sticky))
        awaitClose { unregisterReceiver(receiver) }
    }.conflate().distinctUntilChanged()

internal fun intentFilterOf(vararg actions: String): IntentFilter = IntentFilter().apply { actions.forEach(::addAction) }
