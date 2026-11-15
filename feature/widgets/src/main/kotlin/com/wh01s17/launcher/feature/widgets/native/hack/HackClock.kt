package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.status.ClockGranularity
import com.wh01s17.launcher.core.system.status.ClockSource
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.time.temporal.IsoFields
import java.util.Locale

/**
 * El reloj hack, como `tty-clock`. Pequeño: la hora en texto con segundos y cursor. Mediano: la hora en
 * cifras de bloques centradas que llenan el ancho, la fecha y el avance del día y del año en barras. Grande:
 * además la marca de tiempo Unix en hex, la semana ISO y el desfase. Completo: también el segundero, `cal` y,
 * al pie, `fortune` (una frase de cine hacker que cambia cada hora).
 */
@Composable
internal fun HackClock(
    clock: ClockSource,
    modifier: Modifier,
) {
    val now by remember(clock) { clock.ticks(ClockGranularity.Second) }.collectAsStateWithLifecycle(clock.now())
    val colors = WhTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    HackFrame(modifier) { size ->
        if (size == WidgetSize.Small) {
            SmallClock(now, locale)
            return@HackFrame
        }
        HackPrompt("tty-clock -sc")
        // En mediano y grande las cifras llenan el alto que sobra (sin pasarse del ancho); a pantalla
        // completa el que se estira es el calendario.
        // Las cifras ocupan el mismo hueco de antes pero a un [DIGITS_SCALE] de su tamaño, centradas: el
        // margen que sobra hace de relleno y el resto del widget no se mueve.
        HackDigits(
            now.format(HOURS),
            colors.accent,
            if (size == WidgetSize.Full) Modifier else Modifier.weight(1f),
            alignment = Alignment.Center,
            scale = DIGITS_SCALE,
        )
        WhText(dateLine(now, locale), maxLines = 1)
        // Cuánto va del día y del año; el segundero, sólo a pantalla completa (así lo prefiere el usuario).
        if (size == WidgetSize.Full) HackMeter("sec ", now.second / SECONDS_PER_MINUTE, ":%02d".format(now.second), colors.cyan)
        val day = now.toLocalTime().toSecondOfDay() / SECONDS_PER_DAY
        val year = (now.dayOfYear - 1 + day) / now.toLocalDate().lengthOfYear()
        HackMeter("day ", day, percent(day), colors.accent)
        HackMeter("year", year, percent(year), colors.cyan)
        if (size >= WidgetSize.Large) {
            HackLine("epoch", "0x%X".format(now.toEpochSecond()))
            HackLine(
                "iso",
                "W%02d-%d d%03d".format(now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), now.get(ChronoField.DAY_OF_WEEK), now.dayOfYear),
            )
            HackLine("tz", "utc${now.offset.id.replace("Z", "+00:00")}")
        }
        if (size == WidgetSize.Full) {
            HackPrompt("cal")
            // El mes ocupa lo que queda, con las semanas repartidas; `fortune` va justo debajo.
            HackCalendar(now.toLocalDate(), locale, Modifier.weight(1f))
            HackFortune(fortuneOf(now.toEpochSecond() / SECONDS_PER_HOUR))
        }
    }
}

/** Pequeño: la hora en texto, los segundos apagados, el cursor y la fecha. */
@Composable
private fun SmallClock(
    now: ZonedDateTime,
    locale: Locale,
) {
    val colors = WhTheme.colors
    val small = WhTheme.typography.title
    Row(verticalAlignment = Alignment.Bottom) {
        WhText(now.format(HOURS), style = WhTheme.typography.displayLarge, color = colors.accent, maxLines = 1)
        WhText(now.format(SECONDS), style = small, color = colors.darkForeground, maxLines = 1)
        HackCursor(small, lit = now.second % 2 == 0)
    }
    WhText(dateLine(now, locale), maxLines = 1)
}

/** `sáb 14-11-2026` (día-mes-año, como lo pidió el usuario). */
private fun dateLine(
    now: ZonedDateTime,
    locale: Locale,
) = now.format(DateTimeFormatter.ofPattern("EEE dd-MM-yyyy", locale)).lowercase(locale)

private fun percent(fraction: Float) = "%3d%%".format((fraction * PERCENT).toInt())

/**
 * El mes en texto, de lunes a domingo; hoy, en vídeo inverso como la selección de una terminal. Con
 * [modifier] de alto fijo (o `weight`), las semanas se reparten ese alto.
 */
@Composable
private fun HackCalendar(
    today: LocalDate,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val colors = WhTheme.colors
    val style = WhTheme.typography.bodySmall
    val first = today.withDayOfMonth(1).with(DayOfWeek.MONDAY)
    val weeks =
        generateSequence(first) { it.plusWeeks(1) }
            .takeWhile { monday ->
                monday.monthValue == today.monthValue || monday.plusDays(DAYS_PER_WEEK - 1).monthValue == today.monthValue
            }.toList()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceEvenly) {
        Row(Modifier.fillMaxWidth()) {
            for (offset in 0L until DAYS_PER_WEEK) {
                val name =
                    first
                        .plusDays(offset)
                        .dayOfWeek
                        .getDisplayName(TextStyle.SHORT, locale)
                        .take(2)
                        .lowercase(locale)
                WhText(name, Modifier.weight(1f), style = style, color = colors.darkForeground, textAlign = TextAlign.Center)
            }
        }
        for (monday in weeks) {
            Row(Modifier.fillMaxWidth()) {
                for (offset in 0L until DAYS_PER_WEEK) {
                    val day = monday.plusDays(offset)
                    val isToday = day == today
                    WhText(
                        if (day.monthValue == today.monthValue) "%2d".format(day.dayOfMonth) else "",
                        Modifier.weight(1f).background(if (isToday) colors.accent else colors.background),
                        style = style,
                        color = if (isToday) colors.background else colors.foreground,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private val HOURS = DateTimeFormatter.ofPattern("HH:mm")
private val SECONDS = DateTimeFormatter.ofPattern(":ss")
private const val DAYS_PER_WEEK = 7L
private const val SECONDS_PER_MINUTE = 60f
private const val SECONDS_PER_DAY = 86_400f
private const val SECONDS_PER_HOUR = 3_600L
private const val PERCENT = 100
private const val DIGITS_SCALE = 0.8f
