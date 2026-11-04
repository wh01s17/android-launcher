package com.wh01s17.launcher.feature.bar.clock

import com.wh01s17.launcher.core.system.status.ClockGranularity
import java.time.ZonedDateTime
import java.time.temporal.IsoFields
import java.util.Locale

/**
 * Formato de fecha como los de `shell.json` (`ddd d MMM HH:mm:ss`, `d MMMM 'W'ww yyyy`), que son
 * los de `Qt.formatDateTime`, para poder compartir la config con el escritorio (plan.md §6).
 *
 * Soportado: `d dd ddd dddd`, `M MM MMM MMMM`, `yy yyyy`, `h hh H HH`, `m mm`, `s ss`, `zzz` (ms),
 * `AP`/`A` y `ap`/`a` (am/pm), y `ww` (semana ISO con dos cifras, como el reloj del shell). El texto
 * entre comillas simples es literal; `''` es una comilla. Con `AP`/`ap`, `h` es de 12 horas.
 * Cualquier otra letra se escribe tal cual.
 */
object ClockFormat {
    private sealed interface Part {
        data class Literal(
            val text: String,
        ) : Part

        data class Token(
            val letter: Char,
            val length: Int,
        ) : Part
    }

    private val TOKEN_LETTERS = setOf('d', 'M', 'y', 'h', 'H', 'm', 's', 'z', 'A', 'a', 'w')

    fun format(
        dateTime: ZonedDateTime,
        pattern: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val parts = parse(pattern)
        val twelveHour = parts.any { it is Part.Token && (it.letter == 'A' || it.letter == 'a') }
        return parts.joinToString("") { part ->
            when (part) {
                is Part.Literal -> part.text
                is Part.Token -> render(part, dateTime, locale, twelveHour)
            }
        }
    }

    /** Cada cuánto cambia el resultado: cada segundo si el formato muestra segundos o milisegundos. */
    fun granularity(pattern: String): ClockGranularity =
        if (parse(
                pattern,
            ).any { it is Part.Token && (it.letter == 's' || it.letter == 'z') }
        ) {
            ClockGranularity.Second
        } else {
            ClockGranularity.Minute
        }

    private fun parse(pattern: String): List<Part> {
        val parts = mutableListOf<Part>()
        val literal = StringBuilder()
        var quoted = false
        var i = 0
        while (i < pattern.length) {
            val c = pattern[i]
            i +=
                when {
                    // `''` es una comilla, dentro o fuera de un tramo entrecomillado.
                    c == '\'' && pattern.getOrNull(i + 1) == '\'' -> literal.append('\'').let { 2 }
                    c == '\'' -> 1.also { quoted = !quoted }
                    quoted || c !in TOKEN_LETTERS -> literal.append(c).let { 1 }
                    else -> {
                        flush(parts, literal)
                        tokenLength(pattern, i).also { parts += Part.Token(c, it) }
                    }
                }
        }
        flush(parts, literal)
        return parts
    }

    private fun flush(
        parts: MutableList<Part>,
        literal: StringBuilder,
    ) {
        if (literal.isNotEmpty()) parts += Part.Literal(literal.toString()).also { literal.clear() }
    }

    /** `AP` y `ap` son un solo símbolo de dos letras; las demás letras se agrupan mientras se repiten. */
    private fun tokenLength(
        pattern: String,
        start: Int,
    ): Int {
        val c = pattern[start]
        val next = pattern.getOrNull(start + 1)
        if ("$c$next" == "AP" || "$c$next" == "ap") return 2
        return pattern.drop(start).takeWhile { it == c }.length
    }

    private fun render(
        token: Part.Token,
        date: ZonedDateTime,
        locale: Locale,
        twelveHour: Boolean,
    ): String =
        when (token.letter) {
            'd' -> ClockNames.day(token.length, date, locale)
            'M' -> ClockNames.month(token.length, date, locale)
            'A', 'a' -> meridiem(token, date)
            else -> number(token, date, twelveHour)
        }

    /** Los símbolos numéricos: año, horas, minutos, segundos, milisegundos y semana ISO. */
    private fun number(
        token: Part.Token,
        date: ZonedDateTime,
        twelveHour: Boolean,
    ): String {
        val length = token.length
        val millis = date.nano / NANOS_PER_MILLI
        return when (token.letter) {
            'y' -> if (length <= TWO_DIGITS) ClockNames.pad(date.year % YEAR_MODULUS, TWO_DIGITS) else date.year.toString()
            'h' -> ClockNames.pad(if (twelveHour) hour12(date.hour) else date.hour, length)
            'H' -> ClockNames.pad(date.hour, length)
            'm' -> ClockNames.pad(date.minute, length)
            's' -> ClockNames.pad(date.second, length)
            'z' -> if (length >= MILLIS_DIGITS) ClockNames.pad(millis, MILLIS_DIGITS) else millis.toString()
            // `ww` es la semana ISO; una `w` suelta o tres no significan nada y se escriben tal cual.
            'w' -> if (length == TWO_DIGITS) ClockNames.pad(date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), TWO_DIGITS) else "w".repeat(length)
            else -> token.letter.toString().repeat(length)
        }
    }

    private fun meridiem(
        token: Part.Token,
        date: ZonedDateTime,
    ): String {
        val text = if (date.hour < HALF_DAY) "AM" else "PM"
        return if (token.letter == 'a') text.lowercase() else text
    }

    private fun hour12(hour: Int): Int = (hour % HALF_DAY).let { if (it == 0) HALF_DAY else it }

    private const val TWO_DIGITS = 2
    private const val MILLIS_DIGITS = 3
    private const val YEAR_MODULUS = 100
    private const val NANOS_PER_MILLI = 1_000_000
    private const val HALF_DAY = 12
}
