package com.wh01s17.launcher.feature.bar.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarModelTest {
    private val today = LocalDate.of(2026, 11, 3)
    private val november = YearMonth.of(2026, 11)

    @Test
    fun `monday weeks start on the monday before the first`() {
        val month = CalendarModel.build(november, DayOfWeek.MONDAY, today)
        assertEquals(CalendarModel.WEEKS, month.weeks.size)
        // El 1 de noviembre de 2026 es domingo: la primera fila empieza el lunes 26 de octubre.
        assertEquals(
            LocalDate.of(2026, 10, 26),
            month.weeks
                .first()
                .days
                .first()
                .date,
        )
        assertEquals(
            LocalDate.of(2026, 12, 6),
            month.weeks
                .last()
                .days
                .last()
                .date,
        )
        assertTrue(month.weeks.all { it.days.size == 7 })
    }

    @Test
    fun `iso week numbers`() {
        val month = CalendarModel.build(november, DayOfWeek.MONDAY, today)
        assertEquals(listOf(44, 45, 46, 47, 48, 49), month.weeks.map { it.isoWeek })
    }

    @Test
    fun `sunday weeks`() {
        val month = CalendarModel.build(november, DayOfWeek.SUNDAY, today)
        assertEquals(
            LocalDate.of(2026, 11, 1),
            month.weeks
                .first()
                .days
                .first()
                .date,
        )
        // Con la semana en domingo, la columna de referencia es el miércoles de esa fila (semana de lunes a domingo).
        assertEquals(listOf(45, 46, 47, 48, 49, 50), month.weeks.map { it.isoWeek })
    }

    @Test
    fun `today and the days of the neighbour months are flagged`() {
        val days = CalendarModel.build(november, DayOfWeek.MONDAY, today).weeks.flatMap { it.days }
        assertEquals(listOf(today), days.filter { it.today }.map { it.date })
        assertEquals(30, days.count { it.inMonth })
        assertEquals(LocalDate.of(2026, 10, 31), days.last { !it.inMonth && it.date.monthValue == 10 }.date)
    }

    @Test
    fun `today does not show in another month unless it is in the grid`() {
        val december = CalendarModel.build(YearMonth.of(2026, 12), DayOfWeek.MONDAY, today).weeks.flatMap { it.days }
        // El 3 de noviembre queda fuera de la cuadrícula de diciembre (empieza el 30 de noviembre).
        assertEquals(emptyList<LocalDate>(), december.filter { it.today }.map { it.date })
        val past = CalendarModel.build(YearMonth.of(2020, 2), DayOfWeek.MONDAY, today)
        assertEquals(29, past.weeks.flatMap { it.days }.count { it.inMonth })
    }

    @Test
    fun `week start parsing and header order`() {
        assertEquals(DayOfWeek.MONDAY, CalendarModel.parseWeekStart(null))
        assertEquals(DayOfWeek.SUNDAY, CalendarModel.parseWeekStart("Sunday"))
        assertEquals(DayOfWeek.SATURDAY, CalendarModel.parseWeekStart(" saturday "))
        assertEquals(DayOfWeek.MONDAY, CalendarModel.parseWeekStart("lunes"))
        assertEquals(
            listOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
            ),
            CalendarModel.weekdays(DayOfWeek.SUNDAY),
        )
    }
}
