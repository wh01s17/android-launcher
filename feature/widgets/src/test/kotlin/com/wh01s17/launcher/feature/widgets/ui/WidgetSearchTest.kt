package com.wh01s17.launcher.feature.widgets.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSearchTest {
    private data class Item(
        val label: String,
        val app: String,
    )

    private val items =
        listOf(
            Item("Pomodoro", "wh01s17"),
            Item("Metrónomo", "wh01s17"),
            Item("Reloj", "wh01s17"),
            Item("Bandeja de entrada", "Gmail"),
            Item("Reloj digital", "Reloj"),
        )

    private val search = WidgetSearch(items, Item::label, Item::app)

    private fun labels(query: String) = search.search(query).map { it.label }

    @Test
    fun `matches names without accents or case`() {
        assertEquals(listOf("Metrónomo"), labels("metronomo"))
        assertEquals(listOf("Pomodoro"), labels("POMO"))
    }

    @Test
    fun `matches the app name too`() {
        assertEquals(listOf("Bandeja de entrada"), labels("gmail"))
    }

    @Test
    fun `every word must match and better matches come first`() {
        assertEquals(listOf("Reloj", "Reloj digital"), labels("reloj"))
        assertEquals(listOf("Reloj digital"), labels("reloj dig"))
        assertEquals(emptyList<String>(), labels("tidal"))
    }
}
