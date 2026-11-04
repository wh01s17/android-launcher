package com.wh01s17.launcher.feature.bar.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.bar.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Los días y semanas atenuados (mes vecino, número de semana). */
private const val MUTED_ALPHA = 0.5f

/**
 * Calendario del reloj (plan.md §5.5): se abre bajo la barra, centrado en la pantalla, y se cierra
 * tocando fuera o con *atrás*.
 */
@Composable
internal fun CalendarPopup(
    today: LocalDate,
    weekStart: DayOfWeek,
    locale: Locale,
    onDismiss: () -> Unit,
) {
    Popup(
        popupPositionProvider = remember { BelowBarPositionProvider },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        CalendarPanel(today, weekStart, locale)
    }
}

/**
 * Un mes con el número de semana ISO, los días en seis filas fijas y hoy marcado. Las flechas
 * cambian de mes; el título vuelve a hoy.
 */
@Composable
internal fun CalendarPanel(
    today: LocalDate,
    weekStart: DayOfWeek,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    var monthIndex by rememberSaveable { mutableLongStateOf(YearMonth.from(today).toMonthIndex()) }
    val month = remember(monthIndex, weekStart, today) { CalendarModel.build(monthOfIndex(monthIndex), weekStart, today) }
    WhPanel(modifier.width(calendarWidth()), contentPadding = WhTheme.spacing.controlGap) {
        MonthHeader(
            month = month.month,
            locale = locale,
            onPrevious = { monthIndex-- },
            onNext = { monthIndex++ },
            onToday = { monthIndex = YearMonth.from(today).toMonthIndex() },
        )
        WeekdayRow(weekStart, locale)
        month.weeks.forEach { week -> WeekRow(week) }
    }
}

@Composable
private fun calendarWidth() = WhTheme.spacing.gridCell * (DAYS + 1) + WhTheme.spacing.controlGap * 2

@Composable
private fun MonthHeader(
    month: YearMonth,
    locale: Locale,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val title = "${month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${month.year}"
    Row(Modifier.width(calendarWidth() - WhTheme.spacing.controlGap * 2), verticalAlignment = Alignment.CenterVertically) {
        NavButton(WhIcons.ArrowLeft, stringResource(R.string.bar_calendar_previous), onPrevious)
        Box(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.bar_calendar_today), onClick = onToday),
            contentAlignment = Alignment.Center,
        ) {
            WhText(title, style = WhTheme.typography.heading, color = WhTheme.colors.accent, maxLines = 1)
        }
        NavButton(WhIcons.ArrowRight, stringResource(R.string.bar_calendar_next), onNext)
    }
}

@Composable
private fun NavButton(
    glyph: WhGlyph,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(WhTheme.spacing.touchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        WhIcon(glyph)
    }
}

@Composable
private fun WeekdayRow(
    weekStart: DayOfWeek,
    locale: Locale,
) {
    val cell = WhTheme.spacing.gridCell
    Row {
        Cell(Modifier.size(cell, cell / 2)) {
            WhText(stringResource(R.string.bar_calendar_week), style = WhTheme.typography.caption, color = WhTheme.colors.darkForeground)
        }
        for (day in CalendarModel.weekdays(weekStart)) {
            Cell(Modifier.size(cell, cell / 2)) {
                WhText(
                    day.getDisplayName(TextStyle.SHORT, locale),
                    style = WhTheme.typography.caption,
                    color = WhTheme.colors.darkForeground,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WeekRow(week: CalendarWeek) {
    val cell = WhTheme.spacing.gridCell
    Row {
        Cell(Modifier.size(cell)) {
            WhText(week.isoWeek.toString(), style = WhTheme.typography.caption, color = WhTheme.colors.darkForeground)
        }
        week.days.forEach { day -> DayCell(day) }
    }
}

@Composable
private fun DayCell(day: CalendarDay) {
    val menu = WhTheme.surfaces.menu
    val colors = WhTheme.colors
    val color: Color = if (day.today) menu.selectedText else colors.foreground
    Cell(
        Modifier
            .size(WhTheme.spacing.gridCell)
            .alpha(if (day.inMonth) 1f else MUTED_ALPHA)
            .then(if (day.today) Modifier.background(menu.selectedBackground, WhTheme.borders.shape) else Modifier),
    ) {
        WhText(day.date.dayOfMonth.toString(), color = color, maxLines = 1)
    }
}

@Composable
private fun Cell(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.Center) { Row(horizontalArrangement = Arrangement.Center) { content() } }
}

/** Bajo la barra y centrado en la ventana, sea cual sea el ancho del reloj que lo abre. */
private object BelowBarPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset((windowSize.width - popupContentSize.width) / 2, anchorBounds.bottom)
}

private const val DAYS = 7

private fun YearMonth.toMonthIndex(): Long = year * MONTHS_PER_YEAR + (monthValue - 1).toLong()

private fun monthOfIndex(index: Long): YearMonth =
    YearMonth.of(
        Math.floorDiv(index, MONTHS_PER_YEAR).toInt(),
        Math.floorMod(index, MONTHS_PER_YEAR).toInt() + 1,
    )

private const val MONTHS_PER_YEAR = 12L
