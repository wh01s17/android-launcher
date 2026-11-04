package com.wh01s17.launcher.feature.settings.shortcuts

import com.wh01s17.launcher.core.config.BarConfig
import com.wh01s17.launcher.core.config.KeyboardConfig
import com.wh01s17.launcher.core.config.LauncherConfig
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.input.binding.Binding
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.core.input.binding.BlockReason
import com.wh01s17.launcher.core.input.binding.ChordParse
import com.wh01s17.launcher.core.input.binding.EncodedCommand
import com.wh01s17.launcher.core.input.binding.Gesture
import com.wh01s17.launcher.core.input.binding.KeyChord
import com.wh01s17.launcher.core.input.binding.KeyModifier
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutTableTest {
    private fun keys(
        text: String,
        command: LauncherCommand,
    ) = Binding(BindingTrigger.Keys((KeyChord.parse(text) as ChordParse.Valid).chord), command)

    private fun config(alternative: KeyModifier?) =
        LauncherConfig(
            version = 1,
            bar = BarConfig(),
            bindings =
                listOf(
                    keys("SUPER + 1", WorkspaceCommand.FocusWorkspace(1)),
                    keys("SUPER + TAB", WorkspaceCommand.CycleWorkspace(forward = true)),
                    keys("CTRL + ALT + A", LauncherCommand.AdjustVolume(1)),
                    Binding(BindingTrigger.Touch(Gesture.SwipeUp), LauncherCommand.OpenMenu),
                ),
            keyboard = KeyboardConfig(alternative),
        )

    @Test
    fun `rows say what to press on this phone and why some cannot be pressed`() {
        val table = ShortcutTable.of(config(KeyModifier.Alt), superDelivered = false)
        assertEquals(listOf("ALT + 1", "SUPER + TAB", "CTRL + ALT + A"), table.keys.map { it.press })
        assertEquals(listOf(null, BlockReason.Recents, null), table.keys.map { it.blocked })
        assertEquals(EncodedCommand("workspace.focus", mapOf("id" to 1)), table.keys.first().command)
        assertEquals(listOf("swipe-up"), table.gestures.map { it.press })
    }

    @Test
    fun `with super delivered the chords are shown as written`() {
        val table = ShortcutTable.of(config(null), superDelivered = true)
        assertEquals(listOf("SUPER + 1", "SUPER + TAB", "CTRL + ALT + A"), table.keys.map { it.press })
        assertEquals(listOf(null, null, null), table.keys.map { it.blocked })
        val noAlternative = ShortcutTable.of(config(null), superDelivered = false)
        assertEquals(BlockReason.NoSuper, noAlternative.keys.first().blocked)
    }
}
