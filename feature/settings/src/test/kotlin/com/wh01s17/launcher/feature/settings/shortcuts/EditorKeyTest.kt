package com.wh01s17.launcher.feature.settings.shortcuts

import android.view.KeyEvent
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.core.input.binding.ChordParse
import com.wh01s17.launcher.core.input.binding.EncodedCommand
import com.wh01s17.launcher.core.input.binding.Gesture
import com.wh01s17.launcher.core.input.binding.KeyChord
import com.wh01s17.launcher.core.input.binding.encode
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorKeyTest {
    private val keyRow =
        ShortcutRow(
            BindingTrigger.Keys((KeyChord.parse("SUPER + K") as ChordParse.Valid).chord),
            "ALT + K",
            null,
            LauncherCommand.ShowShortcuts.encode(),
        )
    private val gestureRow = ShortcutRow(BindingTrigger.Touch(Gesture.SwipeUp), "swipe-up", null, EncodedCommand("menu.open"))

    @Test
    fun `keys are captured, escape closes and a lone modifier is ignored`() {
        assertEquals(EditorKey.Capture(KeyChord(KeyEvent.KEYCODE_F5)), EditorKey.of(KeyEvent.KEYCODE_F5, 0, keyRow))
        assertEquals(
            "escape with a modifier is a chord like any other",
            EditorKey.Capture((KeyChord.parse("ALT + ESCAPE") as ChordParse.Valid).chord),
            EditorKey.of(KeyEvent.KEYCODE_ESCAPE, KeyEvent.META_ALT_ON, keyRow),
        )
        assertEquals(EditorKey.Dismiss, EditorKey.of(KeyEvent.KEYCODE_ESCAPE, 0, keyRow))
        assertEquals(EditorKey.Ignore, EditorKey.of(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_ON, keyRow))
        assertEquals("a gesture row captures nothing", EditorKey.Ignore, EditorKey.of(KeyEvent.KEYCODE_F5, 0, gestureRow))
        assertEquals(EditorKey.Dismiss, EditorKey.of(KeyEvent.KEYCODE_ESCAPE, 0, gestureRow))
    }

    @Test
    fun `the view model keeps the edited row and the captured chord`() {
        val table = ShortcutTable(listOf(keyRow), listOf(gestureRow), superAlternative = null, superDelivered = false)
        val editor = ShortcutEditorViewModel()
        assertEquals("closed: keys go on", false, editor.onKey(KeyEvent.KEYCODE_F5, 0, table))
        editor.open(keyRow)
        assertEquals(true, editor.onKey(KeyEvent.KEYCODE_F5, 0, table))
        assertEquals(EditorState(keyRow.trigger, KeyChord(KeyEvent.KEYCODE_F5)), editor.state.value)
        editor.onKey(KeyEvent.KEYCODE_ESCAPE, 0, table)
        assertEquals(EditorState(), editor.state.value)
    }
}
