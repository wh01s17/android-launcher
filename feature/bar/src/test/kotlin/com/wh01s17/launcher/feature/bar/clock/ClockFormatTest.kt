package com.wh01s17.launcher.feature.bar.clock

import com.wh01s17.launcher.core.system.status.ClockGranularity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class ClockFormatTest {
    // Martes 3 de noviembre de 2026, 21:54:51.123 (semana ISO 45).
    private val tuesday = ZonedDateTime.of(2026, 11, 3, 21, 54, 51, 123_000_000, ZoneId.of("UTC"))
    private val morning = ZonedDateTime.of(2026, 1, 5, 9, 7, 3, 0, ZoneId.of("UTC"))
    private val en = Locale.ENGLISH

    private fun format(
        pattern: String,
        date: ZonedDateTime = tuesday,
    ) = ClockFormat.format(date, pattern, en)

    @Test
    fun `the formats of shell json`() {
        assertEquals("Tue 3 Nov 21:54:51", format("ddd d MMM HH:mm:ss"))
        assertEquals("3 November W45 2026", format("d MMMM 'W'ww yyyy"))
        assertEquals("Tuesday 21:54", format("dddd HH:mm"))
        assertEquals("21\n—\n54", format("HH\n—\nmm"))
        assertEquals("03\nNov\nW45\n'26", format("dd\nMMM\n'W'ww\n''yy"))
    }

    @Test
    fun `numbers with and without padding`() {
        assertEquals("5/1 9:7:3", format("d/M H:m:s", morning))
        assertEquals("05/01 09:07:03", format("dd/MM HH:mm:ss", morning))
        assertEquals("26 2026", format("yy yyyy"))
        assertEquals("123 123", format("z zzz"))
        assertEquals("007", format("zzz", morning.plusNanos(7_000_000)))
    }

    @Test
    fun `twelve hour clock when am pm is present`() {
        assertEquals("9:54 PM", format("h:mm AP"))
        assertEquals("09:07 am", format("hh:mm ap", morning))
        assertEquals("12:00 AM", format("h:mm A", morning.withHour(0).withMinute(0)))
        assertEquals("12:30 PM", format("h:mm A", morning.withHour(12).withMinute(30)))
        // Sin AM/PM, `h` es de 24 horas.
        assertEquals("21", format("h"))
    }

    @Test
    fun `quotes and unknown letters`() {
        assertEquals("hora: 21:54", format("'hora': HH:mm"))
        assertEquals("it's 21", format("'it''s' HH"))
        assertEquals("x 9", format("x H", morning))
        // Una `w` sola o tres no son semana ISO.
        assertEquals("w www", format("w www"))
        assertEquals("", format(""))
    }

    @Test
    fun `week number follows iso weeks across the new year`() {
        val january1 = ZonedDateTime.of(2027, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")) // viernes: semana 53 de 2026
        assertEquals("53", format("ww", january1))
        assertEquals("01", format("ww", ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))))
    }

    @Test
    fun `granularity depends on the seconds`() {
        assertEquals(ClockGranularity.Second, ClockFormat.granularity("ddd d MMM HH:mm:ss"))
        assertEquals(ClockGranularity.Second, ClockFormat.granularity("HH:mm:ss.zzz"))
        assertEquals(ClockGranularity.Minute, ClockFormat.granularity("dddd HH:mm"))
        // Una `s` entre comillas es texto.
        assertEquals(ClockGranularity.Minute, ClockFormat.granularity("HH:mm 'segundos'"))
    }
}
