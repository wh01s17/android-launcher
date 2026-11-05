package com.wh01s17.launcher.feature.notifications

import java.util.concurrent.TimeUnit

/** Cuánto hace que llegó una notificación, en la unidad mayor que cabe (`ahora`, minutos, horas o días). */
internal sealed interface NotificationAge {
    data object Now : NotificationAge

    data class Minutes(
        val value: Int,
    ) : NotificationAge

    data class Hours(
        val value: Int,
    ) : NotificationAge

    data class Days(
        val value: Int,
    ) : NotificationAge

    companion object {
        fun of(
            now: Long,
            postTime: Long,
        ): NotificationAge {
            val elapsed = (now - postTime).coerceAtLeast(0)
            return when {
                elapsed < TimeUnit.MINUTES.toMillis(1) -> Now
                elapsed < TimeUnit.HOURS.toMillis(1) -> Minutes(TimeUnit.MILLISECONDS.toMinutes(elapsed).toInt())
                elapsed < TimeUnit.DAYS.toMillis(1) -> Hours(TimeUnit.MILLISECONDS.toHours(elapsed).toInt())
                else -> Days(TimeUnit.MILLISECONDS.toDays(elapsed).toInt())
            }
        }
    }
}
