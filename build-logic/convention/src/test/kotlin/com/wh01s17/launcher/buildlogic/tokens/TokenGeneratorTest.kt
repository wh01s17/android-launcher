package com.wh01s17.launcher.buildlogic.tokens

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.tomlj.Toml
import java.io.File

class TokenGeneratorTest {
    @TempDir
    lateinit var dir: File

    private fun write(
        name: String,
        text: String,
    ) = File(dir, name).writeText(text)

    private fun theme(
        colors: String = """
            background = "#0a0e0f"
            dark_background = "#070a0b"
            hyprland_active_border = "rgba(84c959ee) rgba(45d9eaee) 45deg"
            hyprland_inactive_border = "rgba(243038aa)"
        """,
        bar: String = """
            [bar]
            background-alpha = 0.96
            border = "hyprland.active-border-foreground"
            size-horizontal = 36
            scale-with-font = true
        """,
        lua: String = "decoration = {\n  rounding = 10,\n  rounding_power = 2,\n}\n",
    ): ThemeSource {
        write("colors.toml", colors)
        write("shell.bar.toml", bar)
        write("hyprland.lua", lua)
        return ThemeSource.load(dir)
    }

    private fun kotlin(source: ThemeSource = theme()) = TokenGenerator.generate(source, "com.example.tokens").kotlin

    @Test
    fun `hex colors become Color with opaque alpha`() {
        val code = kotlin()
        assertTrue("val background = Color(0xFF0A0E0F)" in code, code)
        assertTrue("val darkBackground = Color(0xFF070A0B)" in code, code)
    }

    @Test
    fun `hyprland rgba stops become ARGB colors and the angle is kept`() {
        val code = kotlin()
        assertTrue("val activeStart = Color(0xEE84C959)" in code, code)
        assertTrue("val activeEnd = Color(0xEE45D9EA)" in code, code)
        assertTrue("const val activeAngleDegrees = 45.0f" in code, code)
        assertTrue("val inactive = Color(0xAA243038)" in code, code)
        assertTrue("val rounding = 10.dp" in code, code)
    }

    @Test
    fun `border keys of colors toml are not duplicated in Colors`() {
        val colors = kotlin().substringAfter("object Colors").substringBefore("object Border")
        assertTrue("hyprland" !in colors.lowercase(), colors)
    }

    @Test
    fun `numbers get their type from the key and the section`() {
        val code = kotlin()
        assertTrue("val backgroundAlpha = 0.96f" in code, code)
        assertTrue("val sizeHorizontal = 36.dp" in code, code)
        assertTrue("val scaleWithFont = true" in code, code)
    }

    @Test
    fun `font section uses sp and scale is a Float`() {
        write("colors.toml", "background = \"#000000\"\nhyprland_active_border = \"rgba(00000000) rgba(00000000) 0deg\"\nhyprland_inactive_border = \"rgba(00000000)\"\n")
        write("shell.font.toml", "[font]\nfamily = \"Departure Mono\"\nbase-size = 12\nscale = 1.0\n")
        write("hyprland.lua", "rounding = 4\n")
        val code = kotlin(ThemeSource.load(dir))
        assertTrue("val family = \"Departure Mono\"" in code, code)
        assertTrue("val baseSize = 12.sp" in code, code)
        assertTrue("val scale = 1.0f" in code, code)
    }

    @Test
    fun `hyprland references become a typed border source`() {
        assertTrue("val border = WhBorderSource.HyprlandActive" in kotlin())
    }

    @Test
    fun `sections are named in PascalCase from their table`() {
        write("shell.image-picker.toml", "[image-picker]\nscrim = \"#0a0e0f\"\nscrim-alpha = 0.72\n")
        val code = kotlin(theme())
        assertTrue("object ImagePicker {" in code, code)
        assertTrue("val scrimAlpha = 0.72f" in code, code)
    }

    @Test
    fun `color resources only include hex colors with a wh prefix`() {
        val xml = TokenGenerator.generate(theme(), "p").colorResources
        assertTrue("""<color name="wh_background">#FF0A0E0F</color>""" in xml, xml)
        assertTrue("""<color name="wh_dark_background">#FF070A0B</color>""" in xml, xml)
        assertTrue("hyprland" !in xml, xml)
    }

    @Test
    fun `output is deterministic`() {
        assertEquals(kotlin(), kotlin())
    }

    @Test
    fun `unknown hyprland reference fails loudly`() {
        val source = theme(bar = "[bar]\nborder = \"hyprland.inactive-border\"\n")
        val error = assertThrows(TokenException::class.java) { kotlin(source) }
        assertTrue("referencia desconocida" in error.message!!)
    }

    @Test
    fun `alpha outside 0 to 1 fails`() {
        val source = theme(bar = "[bar]\nbackground-alpha = 1.5\n")
        assertThrows(TokenException::class.java) { kotlin(source) }
    }

    @Test
    fun `a missing rounding line fails`() {
        write("colors.toml", "background = \"#000000\"\n")
        write("shell.bar.toml", "[bar]\ntext = \"#000000\"\n")
        write("hyprland.lua", "-- nothing here\n")
        assertThrows(TokenException::class.java) { ThemeSource.load(dir) }
    }

    @Test
    fun `a shell file with two tables fails`() {
        write("colors.toml", "background = \"#000000\"\n")
        write("hyprland.lua", "rounding = 1\n")
        write("shell.bar.toml", "[bar]\na = 1\n[other]\nb = 2\n")
        assertThrows(TokenException::class.java) { ThemeSource.load(dir) }
    }

    @Test
    fun `invalid toml reports the file`() {
        write("colors.toml", "background = \n")
        write("shell.bar.toml", "[bar]\n")
        write("hyprland.lua", "rounding = 1\n")
        val error = assertThrows(TokenException::class.java) { ThemeSource.load(dir) }
        assertTrue("colors.toml" in error.message!!)
    }

    @Test
    fun `camel and pascal conversions`() {
        assertEquals("darkBackground", TokenGenerator.camel("dark_background"))
        assertEquals("normalFillAlpha", TokenGenerator.camel("normal-fill-alpha"))
        assertEquals("ImagePicker", TokenGenerator.pascal("image-picker"))
    }

    @Test
    fun `toml parser keeps types the generator relies on`() {
        val table = Toml.parse("a = 1\nb = 1.0\nc = true\nd = \"x\"\n")
        assertTrue(table.get("a") is Long, "los enteros llegan como Long")
        assertTrue(table.get("b") is Double, "los decimales llegan como Double")
    }

    @Test
    fun `the real theme generates every token the design system uses`() {
        val real = File("../../theme/wh01s17")
        if (!real.isDirectory) return // fuera del repo (p. ej. copia parcial): nada que comprobar
        val code = TokenGenerator.generate(ThemeSource.load(real), "p").kotlin
        assertTrue("val accent = Color(0xFF84C959)" in code)
        assertTrue("val background = Color(0xFF0A0E0F)" in code)
        assertTrue("val activeEnd = Color(0xEE45D9EA)" in code)
        for (section in listOf("Bar", "Controls", "Font", "ImagePicker", "Launcher", "Lock", "Menu", "Notifications", "Polkit", "Popups", "Spacing", "Tooltip")) {
            assertTrue("object $section {" in code, "falta $section")
        }
    }
}
