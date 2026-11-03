package com.wh01s17.launcher.core.fuzzy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FuzzyIndexTest {
    private data class App(
        val label: String,
        val pkg: String = label.lowercase().replace(" ", ""),
    )

    private fun index(vararg labels: String) = FuzzyIndex(labels.map { App(it) }, { it.label })

    private fun FuzzyIndex<App>.labels(
        query: String,
        usage: (App) -> Usage? = { null },
        now: Long = 0,
    ) = search(query, usage, now).map { it.item.label }

    @Test
    fun `query must be a subsequence`() {
        val apps = index("Firefox", "Files", "Maps")
        assertEquals(listOf("Firefox"), apps.labels("ffx"))
        assertEquals(emptyList<String>(), apps.labels("xyz"))
    }

    @Test
    fun `word starts beat the middle of a word`() {
        assertEquals(listOf("YouTube Music", "Emulator"), index("Emulator", "YouTube Music").labels("mu"))
    }

    @Test
    fun `prefixes come first`() {
        val result = index("Escala", "Calendario", "Calculadora").labels("cal")
        assertEquals("Escala", result.last())
    }

    @Test
    fun `consecutive letters beat scattered ones`() {
        assertEquals(listOf("Chrome", "Character Hub"), index("Character Hub", "Chrome").labels("chr"))
    }

    @Test
    fun `camel case counts as a word start`() {
        assertEquals(
            listOf("YouTube", "Yesterday Tasks"),
            index("Yesterday Tasks", "YouTube").labels("yt").take(2).sortedBy {
                it !=
                    "YouTube"
            },
        )
        val tube = index("YouTube").search("t").single()
        assertEquals(listOf(3), tube.positions.toList())
    }

    @Test
    fun `accents and case are ignored and positions point at the original`() {
        val result = index("Música").search("MUSICA").single()
        assertEquals(listOf(0, 1, 2, 3, 4, 5), result.positions.toList())
        assertEquals(listOf("Música"), index("Música").labels("mus"))
    }

    @Test
    fun `several words must all match`() {
        val apps = index("Google Maps", "Google Fotos", "Maps.me")
        assertEquals(listOf("Google Maps"), apps.labels("goo map"))
        val positions =
            apps
                .search("goo map")
                .first()
                .positions
                .toList()
        assertEquals(listOf(0, 1, 2, 7, 8, 9), positions)
    }

    @Test
    fun `an exact name wins`() {
        assertEquals("Calendario", index("Calendario Google", "Calendario").labels("calendario").first())
    }

    @Test
    fun `keywords match with a penalty and without positions`() {
        val apps = FuzzyIndex(listOf(App("Ajustes", "com.android.settings"), App("Settings Pro")), { it.label }, { listOf(it.pkg) })
        val result = apps.search("settings")
        assertEquals(listOf("Settings Pro", "Ajustes"), result.map { it.item.label })
        assertEquals(0, result.last().positions.size)
    }

    @Test
    fun `keywords need the term in one piece`() {
        val apps =
            FuzzyIndex(
                listOf(App("Maps", "com.google.android.apps.maps"), App("Google One", "com.google.android.apps.subscriptions.red")),
                { it.label },
                { listOf(it.pkg) },
            )
        // "map" está disperso en el paquete de Google One (m de com, a-p de apps): no cuenta.
        assertEquals(listOf("Maps"), apps.search("goo map").map { it.item.label })
    }

    @Test
    fun `usage breaks ties and decays with time`() {
        val apps = index("Telegram", "Teams")
        val day = 24 * 60 * 60 * 1000L
        val used = { app: App -> if (app.label == "Teams") Usage(launchCount = 10, lastUsedAtMillis = 0) else null }
        assertEquals("Teams", apps.labels("te", used, now = 0).first())
        assertTrue(FuzzyIndex.usageBonus(Usage(10, 0), 0) > FuzzyIndex.usageBonus(Usage(10, 0), 30 * day))
        assertEquals(0, FuzzyIndex.usageBonus(Usage(0, 0), 0))
        assertEquals(0, FuzzyIndex.usageBonus(null, 0))
    }

    @Test
    fun `usage does not override a clearly better match`() {
        val apps = index("Calculadora", "Clock")
        val clockHeavy = { app: App -> if (app.label == "Clock") Usage(1000, 0) else null }
        assertEquals("Calculadora", apps.labels("calcu", clockHeavy).first())
    }

    @Test
    fun `empty query lists everything by usage then by input order`() {
        val apps = index("B", "A", "C")
        assertEquals(listOf("B", "A", "C"), apps.labels("  "))
        assertEquals(listOf("C", "B", "A"), apps.labels("", { if (it.label == "C") Usage(3, 0) else null }))
    }

    @Test
    fun `limit cuts the list`() {
        assertEquals(2, index("aa", "ab", "ac").search("a", limit = 2).size)
        assertEquals(0, index("aa").search("a", limit = -1).size)
    }

    @Test
    fun `query longer than the text never matches`() {
        assertEquals(emptyList<String>(), index("ab").labels("abc"))
    }
}
