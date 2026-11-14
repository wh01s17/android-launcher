package com.wh01s17.launcher.core.apps.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LabelCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file by lazy { File(folder.root, "labels.tsv") }

    private fun key(
        component: String,
        locale: String = "es-CL",
    ) = LabelCache.key(component, 0, "/data/app/a/base.apk", locale)

    @Test
    fun `labels survive a restart without loading them again`() {
        LabelCache(file).apply {
            assertEquals("Notas", label(key("com.notes/.Main")) { "Notas" })
            save()
        }
        var loads = 0
        val restarted = LabelCache(file)
        assertEquals("Notas", restarted.label(key("com.notes/.Main")) { "otro".also { loads++ } })
        assertEquals(0, loads)
    }

    @Test
    fun `an update or a new language reads the label again and drops the old key`() {
        LabelCache(file).apply {
            label(key("com.notes/.Main")) { "Notas" }
            label(key("com.maps/.Main")) { "Mapas" }
            save()
        }
        LabelCache(file).apply {
            assertEquals("Notes", label(key("com.notes/.Main", locale = "en-US")) { "Notes" })
            save()
        }
        val lines = file.readLines()
        assertEquals(lines.toString(), 2, lines.size)
        assertTrue(lines.any { it.contains("en-US") && it.endsWith("Notes") })
        // La otra app no se leyó esta vez y se conserva.
        assertTrue(lines.any { it.endsWith("Mapas") })
    }

    @Test
    fun `tabs, newlines and backslashes round-trip`() {
        val odd = "a\tb\nc\\d"
        LabelCache(file).apply {
            label(key("com.odd/.Main")) { odd }
            save()
        }
        assertEquals(odd, LabelCache(file).label(key("com.odd/.Main")) { "x" })
    }

    @Test
    fun `an unreadable cache is empty, not an error`() {
        file.writeText("garbage-without-tab\n\t\n")
        assertEquals("Notas", LabelCache(file).label(key("com.notes/.Main")) { "Notas" })
    }
}
