package com.wh01s17.launcher.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ConfigParserTest {
    private fun success(
        text: String,
        fallback: LauncherConfig? = null,
    ) = (ConfigParser.parse(text, fallback) as ParseResult.Success).config

    private fun errors(
        text: String,
        fallback: LauncherConfig? = null,
    ) = (ConfigParser.parse(text, fallback) as ParseResult.Failure).errors.map {
        it.path to
            it.message
    }

    @Test
    fun `the factory config is valid and its bar does not repeat the system status bar`() {
        val config = success(File("src/main/assets/default-launcher.json").readText())
        assertEquals(1, config.version)
        // Hora, red, bluetooth y batería ya están en la barra de estado de Android: la de fábrica no
        // los repite (los módulos siguen disponibles para quien los añada en su config).
        assertEquals(
            listOf(
                "core.menu",
                "core.workspaces",
                "wh01s17.indicators",
                "wh01s17.clock",
                "wh01s17.audio",
            ),
            config.bar.layout.all
                .map { it.id },
        )
        // La fecha, a la derecha junto al volumen (la hora ya está en la barra de Android); mantener
        // pulsado alterna a la hora. Sin grupo central.
        assertEquals(null, config.bar.centerAnchor)
        assertTrue(
            config.bar.layout.center
                .isEmpty(),
        )
        val clock =
            config.bar.layout.right
                .single { it.id == "wh01s17.clock" }
        assertEquals("ddd d MMM", clock.string("format"))
        assertEquals("HH:mm", clock.string("formatAlt"))
    }

    @Test
    fun `module options keep their types`() {
        val config =
            success(
                """{"version":1,"bar":{"layout":{"left":[{"id":"git","brackets":true,"dimClasses":["clean"],"interval":30,
                "classColors":{"dirty":"#e8b53f","default":"#abc"},"bracketColor":"#33454f"}]}}}""",
            )
        val module =
            config.bar.layout.left
                .single()
        assertEquals("git", module.id)
        assertEquals(true, module.bool("brackets"))
        assertEquals(30, module.int("interval"))
        assertEquals(listOf("clean"), module.stringList("dimClasses"))
        assertEquals(mapOf("dirty" to "#e8b53f", "default" to "#abc"), module.stringMap("classColors"))
        assertNull(module.string("missing"))
        assertNull(module.bool("interval"))
    }

    @Test
    fun `bar defaults match shell json`() {
        val bar = success("""{"version":1,"bar":{}}""").bar
        assertEquals(BarPosition.Top, bar.position)
        assertEquals(true, bar.transparent)
        assertEquals(false, bar.outline)
        assertNull(bar.centerAnchor)
        assertTrue(bar.layout.all.isEmpty())
    }

    @Test
    fun `a partial bar only changes what it says and keeps the rest of the fallback`() {
        val fallback = success(File("src/main/assets/default-launcher.json").readText())
        val outline = success("""{"version":1,"bar":{"outline":true}}""", fallback).bar
        assertEquals(true, outline.outline)
        assertEquals(fallback.bar.layout, outline.layout)
        // El ancla también se hereda (la barra de fábrica no tiene; se prueba con un respaldo que sí).
        val anchored = success("""{"version":1,"bar":{"centerAnchor":"wh01s17.clock"}}""", fallback)
        assertEquals("wh01s17.clock", success("""{"version":1,"bar":{"outline":true}}""", anchored).bar.centerAnchor)
        // Un grupo nuevo sustituye sólo ese grupo; `[]` lo vacía.
        val layout = success("""{"version":1,"bar":{"layout":{"right":[]}}}""", fallback).bar.layout
        assertEquals(fallback.bar.layout.left, layout.left)
        assertEquals(fallback.bar.layout.center, layout.center)
        assertTrue(layout.right.isEmpty())
        // `centerAnchor` explícito manda, aunque sea otro.
        assertEquals("core.menu", success("""{"version":1,"bar":{"centerAnchor":"core.menu"}}""", fallback).bar.centerAnchor)
    }

    @Test
    fun `a missing bar comes from the fallback, and without a fallback is an error`() {
        val fallback = success("""{"version":1,"bar":{"outline":true}}""")
        assertEquals(true, success("""{"version":1}""", fallback).bar.outline)
        assertEquals(listOf("bar" to "falta"), errors("""{"version":1}"""))
    }

    @Test
    fun `unknown keys are ignored so newer configs still open`() {
        val config = success("""{"version":1,"idle":{"screensaver":150},"future":[1,2],"bar":{"newOption":3}}""")
        assertEquals(1, config.version)
    }

    @Test
    fun `version problems say what is wrong`() {
        assertEquals(listOf("version" to "falta"), errors("""{"bar":{}}"""))
        assertEquals(listOf("version" to "debe ser un número entero"), errors("""{"version":"1","bar":{}}"""))
        assertEquals(listOf("version" to "debe ser un número entero"), errors("""{"version":1.5,"bar":{}}"""))
        assertEquals(listOf("version" to "debe ser 1 o más"), errors("""{"version":0,"bar":{}}"""))
        assertTrue(errors("""{"version":2,"bar":{}}""").single().second.contains("más nueva"))
    }

    @Test
    fun `all the errors come together with their paths`() {
        val found =
            errors(
                """{"version":1,"bar":{"position":"left","transparent":"yes","layout":{"left":[{"brackets":1},{"id":3},"menu"],
                "center":{}, "right":[{"id":"x","classColors":{"a":"red"},"dimClasses":"a","bracketColor":"#12"}]}}}""",
            ).toMap()
        assertTrue(found.getValue("bar.position").contains("«left» no es válido"))
        assertEquals("debe ser true o false", found["bar.transparent"])
        assertEquals("falta", found["bar.layout.left[0].id"])
        assertEquals("debe ser true o false", found["bar.layout.left[0].brackets"])
        assertEquals("debe ser un texto", found["bar.layout.left[1].id"])
        assertEquals("debe ser un objeto con «id»", found["bar.layout.left[2]"])
        assertEquals("debe ser una lista", found["bar.layout.center"])
        assertTrue(found.getValue("bar.layout.right[0].classColors.a").startsWith("debe ser un color"))
        assertEquals("debe ser una lista de textos", found["bar.layout.right[0].dimClasses"])
        assertTrue(found.getValue("bar.layout.right[0].bracketColor").startsWith("debe ser un color"))
    }

    @Test
    fun `broken json and wrong roots are reported, not thrown`() {
        assertTrue(errors("{ nope").single().second.startsWith("JSON no válido"))
        assertTrue(errors("").single().second.startsWith("JSON no válido"))
        assertEquals(listOf("$" to "debe ser un objeto JSON"), errors("[1]"))
    }

    @Test
    fun `hex colors`() {
        assertEquals(0xFF84C959.toInt(), HexColor.parse("#84c959"))
        assertEquals(0xFFAABBCC.toInt(), HexColor.parse("#abc"))
        assertEquals(0xEE84C959.toInt(), HexColor.parse("#84c959ee"))
        for (bad in listOf("84c959", "#84c95", "#xyz123", "", "#", "#1234")) assertNull(bad, HexColor.parse(bad))
    }
}
