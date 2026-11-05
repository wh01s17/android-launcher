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
 * fecha. Mediano: además la semana de lunes a domingo con hoy subrayado. Grande: el mes entero,
 * como el popup del reloj de la barra (la semana empieza en lunes).
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
        Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            if (size != WidgetSize.Small) {
                NovaHeading(
                    stringResource(R.string.native_clock_title),
                    stringResource(R.string.native_clock_week, today.get(WeekFields.ISO.weekOfWeekBasedYear())),
                )
            }
            WhText(
                now.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = WhTheme.typography.displayLarge,
                color = colors.accent,
                maxLines = 1,
            )
            WhText(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)), color = colors.foreground, maxLines = 1)
            when (size) {
                WidgetSize.Small -> Unit
                WidgetSize.Medium -> Week(today.with(DayOfWeek.MONDAY), today, locale, header = true)
                WidgetSize.Large, WidgetSize.Full -> Month(today, locale)
            }
        }
    }
}

/** El mes de [today] en semanas de lunes a domingo; los días de otros meses, en blanco. */
@Composable
private fun Month(
    today: LocalDate,
    locale: Locale,
) {
    val first = today.withDayOfMonth(1)
    var monday = first.with(DayOfWeek.MONDAY)
    Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap / 2)) {
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
