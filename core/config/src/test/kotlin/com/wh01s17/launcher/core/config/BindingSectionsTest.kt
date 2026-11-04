package com.wh01s17.launcher.core.config

import android.view.KeyEvent
import com.wh01s17.launcher.core.input.CommandDirection
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.input.binding.Binding
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.core.input.binding.ChordParse
import com.wh01s17.launcher.core.input.binding.Gesture
import com.wh01s17.launcher.core.input.binding.GestureBindingResolver
import com.wh01s17.launcher.core.input.binding.KeyBindingResolver
import com.wh01s17.launcher.core.input.binding.KeyChord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class BindingSectionsTest {
    private val factory by lazy {
        (ConfigParser.parse(File("src/main/assets/default-launcher.json").readText()) as ParseResult.Success).config
    }

    private fun success(text: String) = (ConfigParser.parse(text, factory) as ParseResult.Success).config

    private fun errors(text: String) = (ConfigParser.parse(text, factory) as ParseResult.Failure).errors.map { it.path to it.message }

    private fun keys(text: String) = BindingTrigger.Keys((KeyChord.parse(text) as ChordParse.Valid).chord)

    private fun commandFor(
        bindings: List<Binding>,
        text: String,
    ) = bindings.firstOrNull { it.trigger == keys(text) }?.command

    @Test
    fun `the factory config carries the hyprland tiling shortcuts and the swipe up`() {
        val bindings = factory.bindings
        val superOn = KeyEvent.META_META_ON
        val resolver = KeyBindingResolver(bindings)
        assertEquals(LauncherCommand.OpenMenu, resolver.resolve(KeyEvent.KEYCODE_SPACE, superOn))
        assertEquals(WorkspaceCommand.FocusWorkspace(10), resolver.resolve(KeyEvent.KEYCODE_0, superOn))
        assertEquals(WorkspaceCommand.MoveTileToWorkspace(3), resolver.resolve(KeyEvent.KEYCODE_3, superOn or KeyEvent.META_SHIFT_ON))
        assertEquals(
            WorkspaceCommand.MoveTileToWorkspace(3, follow = false),
            resolver.resolve(KeyEvent.KEYCODE_3, superOn or KeyEvent.META_SHIFT_ON or KeyEvent.META_ALT_ON),
        )
        assertEquals(WorkspaceCommand.MoveFocus(CommandDirection.Left), resolver.resolve(KeyEvent.KEYCODE_DPAD_LEFT, superOn))
        assertEquals(WorkspaceCommand.ResizeTile(-20, 0), resolver.resolve(KeyEvent.KEYCODE_MINUS, superOn))
        assertEquals(LauncherCommand.OpenMenu, GestureBindingResolver(bindings).resolve(Gesture.SwipeUp))
        assertEquals("every trigger appears once", bindings.size, bindings.map { it.trigger }.toSet().size)
    }

    @Test
    fun `user bindings replace, remove or add to the factory ones and keep the rest`() {
        val bindings =
            success(
                """
                {"version":1,"bindings":[
                  {"keys":"SUPER + 1","command":"workspace.focus","args":{"id":5}},
                  {"keys":"SUPER + W","command":"none"},
                  {"gesture":"swipe-down","command":"notifications.open"},
                  {"keys":"F2","command":"edit.toggle"}
                ]}
                """.trimIndent(),
            ).bindings
        assertEquals(WorkspaceCommand.FocusWorkspace(5), commandFor(bindings, "SUPER + 1"))
        assertNull(commandFor(bindings, "SUPER + W"))
        assertEquals(WorkspaceCommand.ToggleEditMode, commandFor(bindings, "F2"))
        assertEquals(LauncherCommand.OpenNotificationShade, GestureBindingResolver(bindings).resolve(Gesture.SwipeDown))
        assertEquals(WorkspaceCommand.FocusWorkspace(2), commandFor(bindings, "SUPER + 2"))
        assertEquals(factory.bindings.size - 1 + 2, bindings.size)
        assertEquals("a replaced binding keeps its place", 1, bindings.indexOfFirst { it.trigger == keys("SUPER + 1") })
        assertEquals(factory.bindings, success("""{"version":1}""").bindings)
    }

    @Test
    fun `wrong bindings point at their path and nothing is applied`() {
        assertEquals(
            listOf(
                "bindings[0]" to "necesita «keys» o «gesture» (sólo uno)",
                "bindings[1].keys" to "«HYPER» no es un modificador; opciones: SUPER, CTRL, ALT, SHIFT",
                "bindings[2].gesture" to "«pinch» no es un gesto; opciones: swipe-up, swipe-down",
                "bindings[3].command" to "falta",
                "bindings[4].args.id" to "debe estar entre 1 y 10",
                "bindings[5].args" to "«none» no lleva argumentos",
                "bindings[6].args.id" to "debe ser un texto, un número entero o true/false",
                "bindings[8]" to "«SUPER + F1» ya tiene un atajo más arriba en la lista",
                "bindings[9]" to "debe ser un objeto con «keys» o «gesture» y «command»",
            ),
            errors(
                """
                {"version":1,"bindings":[
                  {"keys":"SUPER + A","gesture":"swipe-up","command":"menu.open"},
                  {"keys":"HYPER + 1","command":"menu.open"},
                  {"gesture":"pinch","command":"menu.open"},
                  {"keys":"SUPER + B"},
                  {"keys":"SUPER + C","command":"workspace.focus","args":{"id":0}},
                  {"keys":"SUPER + D","command":"none","args":{"id":1}},
                  {"keys":"SUPER + E","command":"workspace.focus","args":{"id":[1]}},
                  {"keys":"super + f1","command":"menu.open"},
                  {"keys":"SUPER + F1","command":"edit.toggle"},
                  "SUPER + G"
                ]}
                """.trimIndent(),
            ),
        )
        assertEquals(listOf("bindings" to "debe ser una lista"), errors("""{"version":1,"bindings":{}}"""))
    }
}
