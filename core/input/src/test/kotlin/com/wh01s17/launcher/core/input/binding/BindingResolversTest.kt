package com.wh01s17.launcher.core.input.binding

import android.view.KeyEvent
import com.wh01s17.launcher.core.input.CommandDirection
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BindingResolversTest {
    private fun keys(
        text: String,
        command: LauncherCommand,
    ) = Binding(BindingTrigger.Keys((KeyChord.parse(text) as ChordParse.Valid).chord), command)

    private val bindings =
        listOf(
            keys("SUPER + 1", WorkspaceCommand.FocusWorkspace(1)),
            keys("SUPER + SHIFT + 1", WorkspaceCommand.MoveTileToWorkspace(1)),
            keys("SHIFT + F1", LauncherCommand.OpenNotificationShade),
            keys("F2", WorkspaceCommand.ToggleEditMode),
            Binding(BindingTrigger.Touch(Gesture.SwipeUp), LauncherCommand.OpenMenu),
        )

    private val keyResolver = KeyBindingResolver(bindings)

    @Test
    fun `modifiers have to match exactly`() {
        val superOn = KeyEvent.META_META_ON or KeyEvent.META_META_RIGHT_ON
        assertEquals(WorkspaceCommand.FocusWorkspace(1), keyResolver.resolve(KeyEvent.KEYCODE_1, superOn))
        assertEquals(WorkspaceCommand.MoveTileToWorkspace(1), keyResolver.resolve(KeyEvent.KEYCODE_1, superOn or KeyEvent.META_SHIFT_ON))
        assertNull(keyResolver.resolve(KeyEvent.KEYCODE_1, superOn or KeyEvent.META_CTRL_ON))
        assertNull(keyResolver.resolve(KeyEvent.KEYCODE_1, 0))
        assertNull(keyResolver.resolve(KeyEvent.KEYCODE_2, superOn))
    }

    @Test
    fun `while typing only chords with super, ctrl or alt apply`() {
        assertEquals(WorkspaceCommand.FocusWorkspace(1), keyResolver.resolve(KeyEvent.KEYCODE_1, KeyEvent.META_META_ON, typing = true))
        assertNull(keyResolver.resolve(KeyEvent.KEYCODE_F1, KeyEvent.META_SHIFT_ON, typing = true))
        assertNull(keyResolver.resolve(KeyEvent.KEYCODE_F2, 0, typing = true))
        assertEquals(LauncherCommand.OpenNotificationShade, keyResolver.resolve(KeyEvent.KEYCODE_F1, KeyEvent.META_SHIFT_ON))
        assertEquals(WorkspaceCommand.ToggleEditMode, keyResolver.resolve(KeyEvent.KEYCODE_F2, 0))
    }

    @Test
    fun `gestures resolve only their own bindings`() {
        val gestures = GestureBindingResolver(bindings)
        assertEquals(LauncherCommand.OpenMenu, gestures.resolve(Gesture.SwipeUp))
        assertNull(gestures.resolve(Gesture.SwipeDown))
    }

    @Test
    fun `commands are built from their name and arguments`() {
        fun valid(
            name: String,
            args: Map<String, Any> = emptyMap(),
        ) = (CommandCatalog.parse(name, args) as CommandParse.Valid).command
        assertEquals(WorkspaceCommand.FocusWorkspace(10), valid("workspace.focus", mapOf("id" to 10)))
        assertEquals(WorkspaceCommand.MoveTileToWorkspace(3, follow = false), valid("tile.move", mapOf("id" to 3, "follow" to false)))
        assertEquals(WorkspaceCommand.MoveTileToWorkspace(3, follow = true), valid("tile.move", mapOf("id" to 3)))
        assertEquals(WorkspaceCommand.SwapTile(CommandDirection.Up), valid("tile.swap", mapOf("direction" to "UP")))
        assertEquals(WorkspaceCommand.ResizeTile(0, -20), valid("tile.resize", mapOf("dy" to -20)))
        assertEquals(LauncherCommand.OpenMenu, valid("menu.open"))
    }

    @Test
    fun `bad commands report every problem with its argument`() {
        fun problems(
            name: String,
            args: Map<String, Any> = emptyMap(),
        ) = (CommandCatalog.parse(name, args) as CommandParse.Invalid).problems.map { it.arg to it.message }
        assertEquals(listOf(null to "«menu.close» no es un comando; ver docs/bindings.md"), problems("menu.close"))
        assertEquals(listOf("id" to "falta"), problems("workspace.focus"))
        assertEquals(listOf("id" to "debe estar entre 1 y 10"), problems("workspace.focus", mapOf("id" to 11)))
        assertEquals(
            listOf(
                "speed" to "«tile.move» no usa este argumento",
                "id" to "debe ser un número entero",
                "follow" to "debe ser true o false",
            ),
            problems("tile.move", mapOf("id" to "3", "follow" to 1, "speed" to 2)),
        )
        assertEquals(
            listOf("direction" to "«north» no es válida; opciones: left, right, up, down"),
            problems("tile.focus", mapOf("direction" to "north")),
        )
    }
}
