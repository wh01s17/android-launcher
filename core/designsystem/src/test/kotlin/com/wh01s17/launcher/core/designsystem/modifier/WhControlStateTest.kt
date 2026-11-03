package com.wh01s17.launcher.core.designsystem.modifier

import com.wh01s17.launcher.core.designsystem.theme.WhControlState.Focus
import com.wh01s17.launcher.core.designsystem.theme.WhControlState.Hover
import com.wh01s17.launcher.core.designsystem.theme.WhControlState.Normal
import com.wh01s17.launcher.core.designsystem.theme.WhControlState.Pressed
import com.wh01s17.launcher.core.designsystem.theme.WhControlState.Selected
import org.junit.Assert.assertEquals
import org.junit.Test

class WhControlStateTest {
    private fun state(
        enabled: Boolean = true,
        pressed: Boolean = false,
        focused: Boolean = false,
        selected: Boolean = false,
        hovered: Boolean = false,
    ) = resolveControlState(enabled, pressed, focused, selected, hovered)

    @Test
    fun `nothing active is normal`() = assertEquals(Normal, state())

    @Test
    fun `hover and selected alone`() {
        assertEquals(Hover, state(hovered = true))
        assertEquals(Selected, state(selected = true))
    }

    @Test
    fun `priority is Pressed over Focus over Selected over Hover`() {
        assertEquals(Pressed, state(pressed = true, focused = true, selected = true, hovered = true))
        assertEquals(Focus, state(focused = true, selected = true, hovered = true))
        assertEquals(Selected, state(selected = true, hovered = true))
    }

    @Test
    fun `a disabled control stays normal whatever happens`() {
        assertEquals(Normal, state(enabled = false, pressed = true, focused = true, selected = true, hovered = true))
    }
}
