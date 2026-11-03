package com.wh01s17.launcher.core.fuzzy

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FuzzyMatcherTest {
    private val matcher = FuzzyMatcher()

    private fun match(
        term: String,
        text: String,
    ) = matcher.match(term.toCharArray(), FoldedText.of(text))

    @Test
    fun `picks the best alignment, not the first one`() {
        // La "m" de "Emulator" aparece antes, pero "Music" empieza palabra.
        assertArrayEquals(intArrayOf(9, 10), match("mu", "Emulator Music")!!.positions)
    }

    @Test
    fun `positions are increasing and point at the letters`() {
        val text = "wh01s17 Launcher Settings"
        val result = match("wls", text)!!
        assertTrue(
            result.positions
                .toList()
                .zipWithNext()
                .all { (a, b) -> a < b },
        )
        assertEquals("wls", result.positions.joinToString("") { text[it].lowercase() })
    }

    @Test
    fun `no match and empty terms`() {
        assertNull(match("zz", "pizza house".replace("z", "")))
        assertNull(match("", "abc"))
    }

    @Test
    fun `buffers grow for long texts`() {
        match("a", "a")
        val long = "x".repeat(300) + "abc"
        assertArrayEquals(intArrayOf(300, 301, 302), match("abc", long)!!.positions)
    }

    @Test
    fun `fold removes accents and keeps the length`() {
        val folded = FoldedText.of("ÁrbolÑandú")
        assertEquals(10, folded.length)
        assertEquals("arbolnandu", folded.folded)
    }
}
