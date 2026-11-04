package com.wh01s17.launcher.feature.bar.module

import androidx.compose.ui.graphics.Color
import com.wh01s17.launcher.core.config.BarModuleConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleStyleResolverTest {
    private val base = Color(0xFFCBD5CE)
    private val green = Color(0xFF84C959)
    private val yellow = Color(0xFFE8B53F)
    private val red = Color(0xFFFF6B66)
    private val muted = Color(0xFF33454F)
    private val defaults = ModuleDefaults(base, muted)

    private fun settings(json: String = "{}") = BarModuleConfig("m", (Json.parseToJsonElement(json) as JsonObject).toMap())

    @Test
    fun `without classes or config the base color is used`() {
        val style = ModuleStyleResolver.resolve(settings(), emptyList(), defaults)
        assertEquals(base, style.color)
        assertFalse(style.dimmed)
        assertFalse(style.brackets)
        assertEquals(muted, style.bracketColor)
    }

    @Test
    fun `the first class with a color wins, in the order of the module`() {
        val cfg = settings("""{"classColors":{"dirty":"#e8b53f","clean":"#84c959"}}""")
        assertEquals(yellow, ModuleStyleResolver.resolve(cfg, listOf("dirty", "clean"), defaults).color)
        assertEquals(green, ModuleStyleResolver.resolve(cfg, listOf("unknown", "clean", "dirty"), defaults).color)
    }

    @Test
    fun `default class is the fallback before the base color`() {
        val cfg = settings("""{"classColors":{"default":"#ff6b66","a":"#84c959"}}""")
        assertEquals(red, ModuleStyleResolver.resolve(cfg, listOf("zzz"), defaults).color)
        assertEquals(red, ModuleStyleResolver.resolve(cfg, emptyList(), defaults).color)
        assertEquals(green, ModuleStyleResolver.resolve(cfg, listOf("a"), defaults).color)
    }

    @Test
    fun `dimClasses dim when any class matches`() {
        val cfg = settings("""{"dimClasses":["inactive","off"]}""")
        assertTrue(ModuleStyleResolver.resolve(cfg, listOf("x", "off"), defaults).dimmed)
        assertFalse(ModuleStyleResolver.resolve(cfg, listOf("x"), defaults).dimmed)
        assertFalse(ModuleStyleResolver.resolve(cfg, emptyList(), defaults).dimmed)
    }

    @Test
    fun `brackets and their color`() {
        val style = ModuleStyleResolver.resolve(settings("""{"brackets":true,"bracketColor":"#84c959"}"""), emptyList(), defaults)
        assertTrue(style.brackets)
        assertEquals(green, style.bracketColor)
    }

    @Test
    fun `module defaults apply and the config overrides them key by key`() {
        val withDefaults = defaults.copy(classColors = mapOf("low" to yellow, "critical" to red), dimClasses = setOf("off"))
        assertEquals(yellow, ModuleStyleResolver.resolve(settings(), listOf("low"), withDefaults).color)
        assertTrue(ModuleStyleResolver.resolve(settings(), listOf("off"), withDefaults).dimmed)
        // Una clave de la config sustituye sólo ese color; los demás se heredan.
        val cfg = settings("""{"classColors":{"low":"#84c959"}}""")
        assertEquals(green, ModuleStyleResolver.resolve(cfg, listOf("low"), withDefaults).color)
        assertEquals(red, ModuleStyleResolver.resolve(cfg, listOf("critical"), withDefaults).color)
        // `dimClasses` de la config sustituye la lista entera, también para vaciarla.
        assertFalse(ModuleStyleResolver.resolve(settings("""{"dimClasses":[]}"""), listOf("off"), withDefaults).dimmed)
    }

    @Test
    fun `an invalid color is ignored instead of breaking the module`() {
        val cfg = settings("""{"classColors":{"a":"verde"},"bracketColor":"x"}""")
        val style = ModuleStyleResolver.resolve(cfg, listOf("a"), defaults)
        assertEquals(base, style.color)
        assertEquals(muted, style.bracketColor)
    }
}
