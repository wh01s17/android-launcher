package com.wh01s17.launcher.core.system.status

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Cada cuánto cambia lo que muestra el reloj: con segundos, cada segundo; si no, cada minuto. */
enum class ClockGranularity(
    val millis: Long,
) {
    Second(MILLIS_PER_SECOND),
    Minute(MILLIS_PER_MINUTE),
}

private const val MILLIS_PER_SECOND = 1_000L
private const val MILLIS_PER_MINUTE = 60_000L

/** La hora actual cada vez que cambia [ClockGranularity], alineada con el reloj (no con el arranque). */
interface ClockSource {
    /** La hora ahora mismo: valor inicial para pintar el primer fotograma sin esperar al primer tick. */
    fun now(): ZonedDateTime

    fun ticks(granularity: ClockGranularity): Flow<ZonedDateTime>
}

/**
 * [ClockSource] sobre la hora del sistema. Además de los ticks, emite al cambiar la hora o la zona
 * horaria. Es un `Flow` frío: sin recolector no hay temporizador (plan.md R: sin trabajo en
 * segundo plano innecesario).
 */
class SystemClockSource(
    private val context: Context,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) : ClockSource {
    override fun now(): ZonedDateTime = nowMillis().toZoned()

    override fun ticks(granularity: ClockGranularity): Flow<ZonedDateTime> {
        val aligned =
            flow {
                while (true) {
                    val now = nowMillis()
                    emit(now.toZoned())
                    delay(millisUntilNext(now, granularity))
                }
            }
        // La difusión emite al registrarse, y `aligned` ya emite al empezar: se descarta el primero.
        val changes =
            context
                .broadcastState(intentFilterOf(Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) { nowMillis().toZoned() }
                .drop(1)
        return merge(aligned, changes)
    }

    private fun Long.toZoned(): ZonedDateTime = Instant.ofEpochMilli(this).atZone(zone())

    companion object {
        /** Milisegundos hasta el siguiente límite de [granularity]; siempre entre 1 y su duración. */
        fun millisUntilNext(
            nowMillis: Long,
            granularity: ClockGranularity,
        ): Long = granularity.millis - Math.floorMod(nowMillis, granularity.millis)
    }
}
