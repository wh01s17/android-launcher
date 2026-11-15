package com.wh01s17.launcher.core.config

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ScreensaverConfigTest {
    private val factory by lazy {
        (ConfigParser.parse(File("src/main/assets/default-launcher.json").readText()) as ParseResult.Success).config
    }

    private fun success(text: String) = (ConfigParser.parse(text, factory) as ParseResult.Success).config.screensaver

    private fun errors(text: String) = (ConfigParser.parse(text, factory) as ParseResult.Failure).errors.map { it.path }

    @Test
    fun `the factory screensaver is the spinning W alone at full brightness`() {
        assertEquals(ScreensaverConfig(), factory.screensaver)
        assertEquals("w", factory.screensaver.style)
    }

    @Test
    fun `every option is read and missing ones keep the factory value`() {
        val config =
            success(
                """{"version":1,"screensaver":{"style":"text","text":"hack the planet","showTime":true,
                "showBattery":true,"spinText":true,"color":"cyan","brightness":40,"speed":150}}""",
            )
        assertEquals(
            ScreensaverConfig(
                style = "text",
                text = "hack the planet",
                showTime = true,
                showBattery = true,
                spinText = true,
                color = "cyan",
                brightness = 40,
                speed = 150,
            ),
            config,
        )
    }

    @Test
    fun `wrong values point at their path`() {
        assertEquals(
            listOf("screensaver.style", "screensaver.text", "screensaver.color", "screensaver.brightness"),
            errors(
                """{"version":1,"screensaver":{"style":"matrix","color":"pink","text":"${"x".repeat(25)}","brightness":5}}""",
            ),
        )
    }
}
