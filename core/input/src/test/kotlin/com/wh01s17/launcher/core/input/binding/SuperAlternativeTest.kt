package com.wh01s17.launcher.core.input.binding

import android.view.KeyEvent
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuperAlternativeTest {
    private fun chord(text: String) = (KeyChord.parse(text) as ChordParse.Valid).chord

    private fun keys(
        text: String,
        command: LauncherCommand,
    ) = Binding(BindingTrigger.Keys(chord(text)), command)

    private val bindings =
        listOf(
            keys("SUPER + 1", WorkspaceCommand.FocusWorkspace(1)),
            keys("SUPER + SHIFT + 1", WorkspaceCommand.MoveTileToWorkspace(1)),
            keys("SUPER + ALT + SPACE", LauncherCommand.OpenAppsMenu),
            keys("SUPER + TAB", WorkspaceCommand.CycleWorkspace(forward = true)),
            keys("SUPER + A", WorkspaceCommand.ToggleLayout),
            keys("CTRL + ALT + A", LauncherCommand.AdjustVolume(1)),
            keys("SUPER + CTRL + A", WorkspaceCommand.ToggleEditMode),
        )

    private val resolver = KeyBindingResolver(bindings, superAlternative = KeyModifier.Alt)

    @Test
    fun `the alternative modifier plays super, but an exact chord wins`() {
        val alt = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        assertEquals(WorkspaceCommand.FocusWorkspace(1), resolver.resolve(KeyEvent.KEYCODE_1, alt))
        assertEquals(WorkspaceCommand.MoveTileToWorkspace(1), resolver.resolve(KeyEvent.KEYCODE_1, alt or KeyEvent.META_SHIFT_ON))
        assertEquals(LauncherCommand.AdjustVolume(1), resolver.resolve(KeyEvent.KEYCODE_A, alt or KeyEvent.META_CTRL_ON))
        assertEquals(WorkspaceCommand.ToggleLayout, resolver.resolve(KeyEvent.KEYCODE_A, alt))
        assertEquals(
            "super itself still works",
            WorkspaceCommand.FocusWorkspace(1),
            resolver.resolve(KeyEvent.KEYCODE_1, KeyEvent.META_META_ON),
        )
        assertNull(KeyBindingResolver(bindings).resolve(KeyEvent.KEYCODE_1, alt))
    }

    @Test
    fun `press for tells what to type, or null when it cannot be typed`() {
        assertEquals(chord("ALT + 1"), resolver.pressFor(chord("SUPER + 1")))
        assertEquals(chord("SUPER + 1"), resolver.pressFor(chord("SUPER + 1"), superDelivered = true))
        assertEquals(chord("CTRL + ALT + A"), resolver.pressFor(chord("CTRL + ALT + A")))
        assertNull("alt cannot appear twice", resolver.pressFor(chord("SUPER + ALT + SPACE")))
        assertNull("alt + tab opens recents", resolver.pressFor(chord("SUPER + TAB")))
        assertNull("ctrl + alt + a is taken by the volume", resolver.pressFor(chord("SUPER + CTRL + A")))
        assertNull("no alternative, no super", KeyBindingResolver(bindings).pressFor(chord("SUPER + 1")))
    }

    @Test
    fun `reserved chords say why`() {
        assertEquals(SystemReservedChords.Reason.SuperNotDelivered, SystemReservedChords.reason(chord("SUPER + 1")))
        assertNull(SystemReservedChords.reason(chord("SUPER + 1"), superDelivered = true))
        assertEquals(SystemReservedChords.Reason.Recents, SystemReservedChords.reason(chord("ALT + TAB")))
        assertNull(SystemReservedChords.reason(chord("ALT + SHIFT + TAB")))
    }
}
