package com.wh01s17.launcher.feature.menu.search

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key

internal class ContextMenuKeys(
    private val move: (Int) -> Unit,
    private val activate: () -> Unit,
    private val dismiss: () -> Unit,
) {
    fun handle(
        code: Int,
        released: Boolean,
        shift: Boolean,
    ): Boolean {
        val direction =
            when (code) {
                KeyEvent.KEYCODE_DPAD_DOWN -> 1
                KeyEvent.KEYCODE_DPAD_UP -> -1
                KeyEvent.KEYCODE_TAB -> if (shift) -1 else 1
                else -> 0
            }
        return when {
            direction != 0 -> {
                if (released) move(direction)
                true
            }
            code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (released) activate()
                true
            }
            code == KeyEvent.KEYCODE_ESCAPE -> {
                if (released) dismiss()
                true
            }
            else -> false
        }
    }
}

internal fun contextKeyCode(key: Key): Int =
    when (key) {
        Key.DirectionDown -> KeyEvent.KEYCODE_DPAD_DOWN
        Key.DirectionUp -> KeyEvent.KEYCODE_DPAD_UP
        Key.Tab -> KeyEvent.KEYCODE_TAB
        Key.Enter -> KeyEvent.KEYCODE_ENTER
        Key.NumPadEnter -> KeyEvent.KEYCODE_NUMPAD_ENTER
        Key.Escape -> KeyEvent.KEYCODE_ESCAPE
        else -> KeyEvent.KEYCODE_UNKNOWN
    }
