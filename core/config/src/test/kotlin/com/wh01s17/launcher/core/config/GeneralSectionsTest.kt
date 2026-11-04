package com.wh01s17.launcher.core.config

import com.wh01s17.launcher.core.tiling.TilingLayout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class GeneralSectionsTest {
    private val factory by lazy {
        (ConfigParser.parse(File("src/main/assets/default-launcher.json").readText()) as ParseResult.Success).config
    }

    private fun success(text: String) = (ConfigParser.parse(text, factory) as ParseResult.Success).config

    private fun errors(text: String) = (ConfigParser.parse(text, factory) as ParseResult.Failure).errors.map { it.path to it.message }

    @Test
    fun `the factory config sets dwindle, eight tiles and the hyprland slide`() {
        assertEquals(GeneralConfig(TilingLayout.Dwindle, 8), factory.general)
        assertEquals(WorkspaceAnimation(enabled = true, curve = AnimationCurve.EaseOutQuint, speed = 2f), factory.animations.workspaces)
        assertEquals(200, factory.animations.workspaces.durationMillis)
    }

    @Test
    fun `general values are read and the missing ones keep the factory or theme value`() {
        val general = success("""{"version":1,"general":{"layout":"master","gapsIn":4,"rounding":0}}""").general
        assertEquals(GeneralConfig(TilingLayout.Master, 8, gapsIn = 4, gapsOut = null, borderSize = null, rounding = 0), general)
        assertEquals(factory.general, success("""{"version":1}""").general)
    }

    @Test
    fun `animation values are read and a disabled animation takes no time`() {
        val animation = success("""{"version":1,"animations":{"workspaces":{"bezier":"quick","speed":3.5}}}""").animations.workspaces
        assertEquals(WorkspaceAnimation(enabled = true, curve = AnimationCurve.Quick, speed = 3.5f), animation)
        assertEquals(350, animation.durationMillis)
        assertEquals(0, success("""{"version":1,"animations":{"workspaces":{"enabled":false}}}""").animations.workspaces.durationMillis)
        assertEquals(factory.animations, success("""{"version":1,"animations":{}}""").animations)
    }

    @Test
    fun `wrong values point at their path`() {
        assertEquals(
            listOf(
                "general.layout" to "«spiral» no es válido; opciones: dwindle, master",
                "general.maxTilesPerWorkspace" to "debe estar entre 1 y 16",
                "general.gapsIn" to "debe ser un número entero",
                "general.borderSize" to "debe estar entre 0 y 8",
            ),
            errors("""{"version":1,"general":{"layout":"spiral","maxTilesPerWorkspace":0,"gapsIn":"2","borderSize":9}}"""),
        )
        assertEquals(
            listOf(
                "animations.workspaces.style" to "«fade» no está disponible; sólo «slide»",
                "animations.workspaces.enabled" to "debe ser true o false",
                "animations.workspaces.bezier" to
                    "«bounce» no es válida; opciones: easeOutQuint, easeInOutCubic, linear, almostLinear, quick",
                "animations.workspaces.speed" to "debe ser un número mayor que 0 y hasta 20",
            ),
            errors("""{"version":1,"animations":{"workspaces":{"style":"fade","enabled":1,"bezier":"bounce","speed":0}}}"""),
        )
        assertEquals(listOf("general" to "debe ser un objeto"), errors("""{"version":1,"general":[]}"""))
        assertEquals(listOf("animations.workspaces" to "debe ser un objeto"), errors("""{"version":1,"animations":{"workspaces":2}}"""))
    }

    @Test
    fun `the terminal is termux by default and takes a package or an activity`() {
        assertEquals("com.termux", factory.apps.terminal)
        assertEquals("org.connectbot", success("""{"version":1,"apps":{"terminal":"org.connectbot"}}""").apps.terminal)
        assertEquals(
            "com.termux/.app.TermuxActivity",
            success("""{"version":1,"apps":{"terminal":"com.termux/.app.TermuxActivity"}}""").apps.terminal,
        )
        assertEquals(
            listOf("apps.terminal" to "debe ser un paquete (com.termux) o una actividad (com.termux/.app.TermuxActivity)"),
            errors("""{"version":1,"apps":{"terminal":"com.termux/"}}"""),
        )
    }
}
