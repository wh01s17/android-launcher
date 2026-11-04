package com.wh01s17.launcher.feature.bar.clock

import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/** Los nombres y números de día y mes de [ClockFormat], con el idioma del sistema. */
internal object ClockNames {
    private const val TWO_DIGITS = 2
    private const val ABBREVIATED = 3

    fun day(
        length: Int,
        date: ZonedDateTime,
        locale: Locale,
    ): String =
        when {
            length == 1 -> date.dayOfMonth.toString()
            length == TWO_DIGITS -> pad(date.dayOfMonth, TWO_DIGITS)
            length == ABBREVIATED -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        }

    fun month(
        length: Int,
        date: ZonedDateTime,
        locale: Locale,
    ): String =
        when {
            length == 1 -> date.monthValue.toString()
            length == TWO_DIGITS -> pad(date.monthValue, TWO_DIGITS)
            length == ABBREVIATED -> date.month.getDisplayName(TextStyle.SHORT, locale)
            else -> date.month.getDisplayName(TextStyle.FULL, locale)
        }

    fun pad(
        value: Int,
        length: Int,
    ): String = value.toString().padStart(length, '0')
}
