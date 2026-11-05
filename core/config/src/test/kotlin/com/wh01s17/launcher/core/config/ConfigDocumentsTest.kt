package com.wh01s17.launcher.core.config

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class ConfigDocumentsTest {
    private class Source(
        var text: String? = null,
        var failWrite: Boolean = false,
    ) : ConfigSource {
        override fun readUserConfig() = text

        override fun readDefaultConfig() = File("src/main/assets/default-launcher.json").readText()

        override fun writeUserConfig(text: String) {
            if (failWrite) throw IOException("disk full")
            this.text = text
        }
    }

    private fun patch(text: String) = Json.parseToJsonElement(text) as JsonObject

    @Test
    fun `style edits preserve unknown fields and bindings and publish immediately`() =
        runTest {
            val original = """{"version":1,"custom":{"value":42},"apps":{"terminal":"com.termux"},
                "bindings":[{"keys":"F2","command":"menu.open"}]}"""
            val source = Source(original)
            val repo = ConfigRepository(source, StandardTestDispatcher(testScheduler))
            repo.reload()
            val bindings =
                repo.state.value.config!!
                    .bindings
            assertEquals(EditResult.Saved, repo.editValues(patch("""{"appearance":{"fontScale":1.5},"apps":{"iconStyle":"pixelated"}}""")))
            val config = repo.state.value.config!!
            assertEquals(1.5f, config.appearance.fontScale)
            assertEquals("pixelated", config.apps.iconStyle)
            assertEquals("com.termux", config.apps.terminal)
            assertEquals(bindings, config.bindings)
            assertEquals(patch(original)["custom"], patch(source.text!!)["custom"])
        }

    @Test
    fun `invalid imports and write failures preserve last good state and original document`() =
        runTest {
            val source = Source("""{"version":1,"general":{"gapsIn":4}}""")
            val repo = ConfigRepository(source, StandardTestDispatcher(testScheduler))
            repo.reload()
            val before = source.text
            val last = repo.state.value
            for (text in listOf("{", """{"version":999}""", """{"version":1,"appearance":{"fontScale":0}}""")) {
                assertTrue(repo.importDocument(text) is EditResult.Refused)
                assertEquals(before, source.text)
                assertEquals(last, repo.state.value)
            }
            source.failWrite = true
            assertTrue(repo.editValues(patch("""{"general":{"gapsIn":8}}""")) is EditResult.Refused)
            assertEquals(last, repo.state.value)
        }

    @Test
    fun `a malformed local file blocks editing but can be replaced with a valid import`() =
        runTest {
            val source = Source("broken")
            val repo = ConfigRepository(source, StandardTestDispatcher(testScheduler))
            assertTrue(repo.editValues(patch("""{"bar":{"transparent":false}}""")) is EditResult.Refused)
            assertEquals("broken", source.text)
            val document = """{"version":1,"idle":{"screensaver":0},"appearance":{"wallpaper":"moon"},"future":true}"""
            assertEquals(EditResult.Saved, repo.importDocument(document))
            assertEquals(document, repo.exportDocument())
            assertEquals(
                0,
                repo.state.value.config!!
                    .idle.screensaver,
            )
            assertEquals(
                "moon",
                repo.state.value.config!!
                    .appearance.wallpaper,
            )
        }

    @Test
    fun `the palette defaults to phosphor and reads a name`() {
        val factory = (ConfigParser.parse(Source().readDefaultConfig()) as ParseResult.Success).config
        assertEquals(AppearanceConfig.DEFAULT_PALETTE, factory.appearance.palette)
        val result = ConfigParser.parse("""{"version":1,"appearance":{"palette":"tokyo-night"}}""", factory)
        assertEquals("tokyo-night", (result as ParseResult.Success).config.appearance.palette)
    }

    @Test
    fun `unsupported theme icon wallpaper and timeout report precise paths`() {
        val factory = (ConfigParser.parse(Source().readDefaultConfig()) as ParseResult.Success).config
        val result =
            ConfigParser.parse(
                """{"version":1,"theme":"missing","apps":{"iconStyle":"broken"},
                "appearance":{"wallpaper":"bad","palette":"Tokyo Night"},"idle":{"screensaver":-1}}""",
                factory,
            )
        assertEquals(
            setOf("theme", "apps.iconStyle", "appearance.wallpaper", "appearance.palette", "idle.screensaver"),
            (result as ParseResult.Failure).errors.map { it.path }.toSet(),
        )
    }

    @Test
    fun `large import is refused and an export without user document contains defaults`() =
        runTest {
            val source = Source()
            val repo = ConfigRepository(source, StandardTestDispatcher(testScheduler))
            assertTrue(repo.importDocument(" ".repeat(ConfigDocuments.MAX_BYTES + 1)) is EditResult.Refused)
            assertNull(source.text)
            assertNotNull(Json.parseToJsonElement(repo.exportDocument()).jsonObject["bindings"])
        }
}
