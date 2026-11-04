package com.wh01s17.launcher.feature.bar.clock

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.IsoFields

/** Un día de la cuadrícula del calendario. */
data class CalendarDay(
    val date: LocalDate,
    /** `false` para los días de los meses vecinos que completan la primera y la última semana. */
    val inMonth: Boolean,
    val today: Boolean,
)

data class CalendarWeek(
    /** Semana ISO (la que contiene el jueves de esa fila si la semana empieza en lunes). */
    val isoWeek: Int,
    val days: List<CalendarDay>,
)

data class CalendarMonth(
    val month: YearMonth,
    val weeks: List<CalendarWeek>,
)

/**
 * El calendario del reloj (plan.md §5.5, "calendario semanal lunes"): siempre [WEEKS] filas, para
 * que el popup no cambie de alto al pasar de mes, y con el primer día de la semana configurable
 * (`weekStartDay`, lunes por defecto).
 */
object CalendarModel {
    const val WEEKS = 6
    private const val DAYS_PER_WEEK = 7
    private const val ISO_REFERENCE_COLUMN = 3

    fun build(
        month: YearMonth,
        weekStart: DayOfWeek,
        today: LocalDate,
    ): CalendarMonth {
        val first = month.atDay(1)
        val offset = Math.floorMod(first.dayOfWeek.value - weekStart.value, DAYS_PER_WEEK)
        val gridStart = first.minusDays(offset.toLong())
        val weeks =
            List(WEEKS) { week ->
                val days =
                    List(DAYS_PER_WEEK) { column ->
                        val date = gridStart.plusDays((week * DAYS_PER_WEEK + column).toLong())
                        CalendarDay(date, inMonth = YearMonth.from(date) == month, today = date == today)
                    }
                // Con la semana en lunes, la cuarta columna es el jueves: la que define la semana ISO.
                CalendarWeek(days[ISO_REFERENCE_COLUMN].date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), days)
            }
        return CalendarMonth(month, weeks)
    }

    /** `monday`, `sunday`… (sin distinguir mayúsculas); desconocido o ausente = lunes. */
    fun parseWeekStart(name: String?): DayOfWeek =
        DayOfWeek.entries.firstOrNull { it.name.equals(name?.trim(), ignoreCase = true) } ?: DayOfWeek.MONDAY

    /** Los siete días empezando por [weekStart], para la cabecera. */
    fun weekdays(weekStart: DayOfWeek): List<DayOfWeek> = List(DAYS_PER_WEEK) { weekStart.plus(it.toLong()) }
}
