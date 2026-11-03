package com.wh01s17.launcher.core.fuzzy

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Objetivo del plan (§5.6, Hito 2): buscar en 500 apps en menos de 16 ms (un fotograma a 60 Hz).
 * Se mide la mediana de muchas búsquedas tras calentar la JVM, con nombres parecidos a los reales.
 */
class FuzzyBenchmarkTest {
    private val words =
        listOf(
            "Google",
            "Maps",
            "Music",
            "Calendar",
            "Camera",
            "Settings",
            "Files",
            "Photos",
            "Chrome",
            "Mail",
            "Bank",
            "Wallet",
            "Notes",
            "Clock",
            "Weather",
            "Player",
            "Pro",
            "Lite",
            "Manager",
            "Keyboard",
            "Música",
            "Cámara",
            "Ajustes",
        )
    private val random = Random(17)
    private val apps = List(500) { List(random.nextInt(1, 4)) { words.random(random) }.joinToString(" ") + " $it" }
    private val queries = listOf("g", "ma", "cam", "set", "mus", "goo map", "clk", "walet", "xyz", "pho lite", "ajus", "1")

    @Test
    fun `500 apps search under 16 ms`() {
        val index = FuzzyIndex(apps, { it }, { listOf("com.example.${it.lowercase().replace(" ", ".")}") })
        repeat(WARMUP) { queries.forEach { q -> index.search(q) } }
        val timings =
            List(RUNS) {
                val query = queries[it % queries.size]
                val start = System.nanoTime()
                index.search(query)
                (System.nanoTime() - start) / 1_000_000.0
            }.sorted()
        val median = timings[timings.size / 2]
        val p95 = timings[timings.size * 95 / 100]
        println("fuzzy 500 apps: median %.3f ms, p95 %.3f ms".format(median, p95))
        assertTrue(median < BUDGET_MS, "median $median ms >= $BUDGET_MS ms")
    }

    private companion object {
        const val WARMUP = 50
        const val RUNS = 600
        const val BUDGET_MS = 16.0
    }
}
