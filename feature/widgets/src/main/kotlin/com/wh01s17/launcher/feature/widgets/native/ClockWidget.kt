package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.WidgetSize
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.status.ClockGranularity
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.feature.widgets.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Reloj y calendario (plan.md §7 Hito 7), con el encabezado nova y la semana ISO. Pequeño: hora y
 * fecha. Mediano: además el avance del día y del año y la semana de lunes a domingo con hoy subrayado,
 * repartidos a lo alto. Grande y completo: la hora más grande y el mes entero, que ocupa lo que queda
 * (la semana empieza en lunes, como el popup del reloj de la barra).
 */
@Composable
internal fun ClockWidget(
    clock: ClockSource,
    modifier: Modifier = Modifier,
) {
    val now by remember(clock) { clock.ticks(ClockGranularity.Minute) }.collectAsStateWithLifecycle(clock.now())
    val colors = WhTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val today = now.toLocalDate()
    BoxWithConstraints(modifier.fillMaxSize().background(colors.background).padding(WhTheme.spacing.controlGap)) {
        val size = WidgetSize.of(maxWidth, maxHeight)
        val gap = Arrangement.spacedBy(WhTheme.spacing.controlGap)
        // En mediano se reparte el alto entero; en grande lo llena el mes.
        Column(Modifier.fillMaxSize(), verticalArrangement = if (size == WidgetSize.Medium) Arrangement.SpaceBetween else gap) {
            if (size != WidgetSize.Small) {
                NovaHeading(
                    stringResource(R.string.native_clock_title),
                    stringResource(R.string.native_clock_week, today.get(WeekFields.ISO.weekOfWeekBasedYear())),
                )
            }
            WhText(
                now.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = if (size == WidgetSize.Small) WhTheme.typography.displayLarge else readoutStyle(size),
                color = colors.accent,
                maxLines = 1,
            )
            WhText(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)), color = colors.foreground, maxLines = 1)
            if (size != WidgetSize.Small) DayAndYear(now.toLocalTime().toSecondOfDay(), today)
            when (size) {
                WidgetSize.Small -> Unit
                WidgetSize.Medium -> Week(today.with(DayOfWeek.MONDAY), today, locale, header = true)
                WidgetSize.Large, WidgetSize.Full -> Month(today, locale, Modifier.weight(1f))
            }
        }
    }
}

/** Cuánto va del día y del año: dos barras finas de nova con su porcentaje encima. */
@Composable
private fun DayAndYear(
    secondOfDay: Int,
    today: LocalDate,
) {
    val colors = WhTheme.colors
    val day = secondOfDay / SECONDS_PER_DAY
    val year = (today.dayOfYear - 1 + day) / today.lengthOfYear()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.panelGap)) {
        val meters = listOf(R.string.native_clock_day to (day to colors.accent), R.string.native_clock_year to (year to colors.cyan))
        meters.forEach { (label, value) ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap / 2)) {
                WhText(
                    stringResource(label, (value.first * PERCENT).toInt()),
                    style = WhTheme.typography.caption,
                    color = colors.darkForeground,
                    maxLines = 1,
                )
                NovaMeter(value.first, value.second)
            }
        }
    }
}

/** El mes de [today] en semanas de lunes a domingo; los días de otros meses, en blanco. */
@Composable
private fun Month(
    today: LocalDate,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val first = today.withDayOfMonth(1)
    var monday = first.with(DayOfWeek.MONDAY)
    // Con alto de sobra (grande, completo), las semanas se reparten ese alto.
    Column(modifier, verticalArrangement = Arrangement.SpaceEvenly) {
        WhText(
            today.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)),
            style = WhTheme.typography.caption,
            color = WhTheme.colors.darkForeground,
        )
        Week(monday, today, locale, header = true, month = today.monthValue)
        monday = monday.plusWeeks(1)
        while (monday.monthValue == today.monthValue) {
            Week(monday, today, locale, header = false, month = today.monthValue)
            monday = monday.plusWeeks(1)
        }
    }
}

/** Siete columnas iguales; con [month], los días de otro mes no se pintan. */
@Composable
private fun Week(
    monday: LocalDate,
    today: LocalDate,
    locale: Locale,
    header: Boolean,
    month: Int? = null,
) {
    val colors = WhTheme.colors
    Row(Modifier.fillMaxWidth()) {
        for (offset in 0L until DAYS_PER_WEEK) {
            val day = monday.plusDays(offset)
            val isToday = day == today
            val visible = month == null || day.monthValue == month
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (header) {
                    WhText(
                        day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        style = WhTheme.typography.caption,
                        color = colors.darkForeground,
                        textAlign = TextAlign.Center,
                    )
                }
                WhText(
                    if (visible) "${day.dayOfMonth}" else "",
                    color = if (isToday) colors.accent else colors.foreground,
                    textAlign = TextAlign.Center,
                )
                Box(
                    Modifier
                        .width(WhTheme.spacing.barUnderlineWidth)
                        .height(WhTheme.spacing.barUnderlineHeight)
                        .background(if (isToday) colors.accent else colors.background),
                )
            }
        }
    }
}

private const val DAYS_PER_WEEK = 7L
private const val SECONDS_PER_DAY = 86_400f
private const val PERCENT = 100
