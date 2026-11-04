package com.wh01s17.launcher.core.input.binding

import android.view.KeyEvent

/**
 * Nombres de las teclas en `launcher.json`, cercanos a los de Hyprland/xkb (`RETURN`, `MINUS`,
 * `EQUAL`). Tabla propia (no `KeyEvent.keyCodeFromString`) para que los nombres no dependan de la
 * versión de Android y se puedan probar sin el framework.
 */
internal object KeyNames {
    private val NAMED: Map<String, Int> =
        mapOf(
            "SPACE" to KeyEvent.KEYCODE_SPACE,
            "RETURN" to KeyEvent.KEYCODE_ENTER,
            "TAB" to KeyEvent.KEYCODE_TAB,
            "ESCAPE" to KeyEvent.KEYCODE_ESCAPE,
            "BACKSPACE" to KeyEvent.KEYCODE_DEL,
            "DELETE" to KeyEvent.KEYCODE_FORWARD_DEL,
            "INSERT" to KeyEvent.KEYCODE_INSERT,
            "HOME" to KeyEvent.KEYCODE_MOVE_HOME,
            "END" to KeyEvent.KEYCODE_MOVE_END,
            "PAGEUP" to KeyEvent.KEYCODE_PAGE_UP,
            "PAGEDOWN" to KeyEvent.KEYCODE_PAGE_DOWN,
            "LEFT" to KeyEvent.KEYCODE_DPAD_LEFT,
            "RIGHT" to KeyEvent.KEYCODE_DPAD_RIGHT,
            "UP" to KeyEvent.KEYCODE_DPAD_UP,
            "DOWN" to KeyEvent.KEYCODE_DPAD_DOWN,
            "MINUS" to KeyEvent.KEYCODE_MINUS,
            "EQUAL" to KeyEvent.KEYCODE_EQUALS,
            "COMMA" to KeyEvent.KEYCODE_COMMA,
            "PERIOD" to KeyEvent.KEYCODE_PERIOD,
            "SLASH" to KeyEvent.KEYCODE_SLASH,
            "BACKSLASH" to KeyEvent.KEYCODE_BACKSLASH,
            "SEMICOLON" to KeyEvent.KEYCODE_SEMICOLON,
            "APOSTROPHE" to KeyEvent.KEYCODE_APOSTROPHE,
            "GRAVE" to KeyEvent.KEYCODE_GRAVE,
            "BRACKETLEFT" to KeyEvent.KEYCODE_LEFT_BRACKET,
            "BRACKETRIGHT" to KeyEvent.KEYCODE_RIGHT_BRACKET,
            "PRINT" to KeyEvent.KEYCODE_SYSRQ,
            "MEDIAPLAYPAUSE" to KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            "MEDIANEXT" to KeyEvent.KEYCODE_MEDIA_NEXT,
            "MEDIAPREVIOUS" to KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        ) +
            ('A'..'Z').associate { it.toString() to KeyEvent.KEYCODE_A + (it - 'A') } +
            ('0'..'9').associate { it.toString() to KeyEvent.KEYCODE_0 + (it - '0') } +
            (1..FUNCTION_KEYS).associate { "F$it" to KeyEvent.KEYCODE_F1 + it - 1 }

    /** Se aceptan al leer, pero se escriben con el nombre de [NAMED]. */
    private val ALIASES: Map<String, Int> =
        mapOf(
            "ENTER" to KeyEvent.KEYCODE_ENTER,
            "ESC" to KeyEvent.KEYCODE_ESCAPE,
            "EQUALS" to KeyEvent.KEYCODE_EQUALS,
            "DEL" to KeyEvent.KEYCODE_FORWARD_DEL,
        )

    private val NAMES: Map<Int, String> = NAMED.entries.associate { (name, code) -> code to name }

    /** Las del teclado numérico se tratan como su tecla normal. */
    private val NUMPAD: Map<Int, Int> =
        mapOf(
            KeyEvent.KEYCODE_NUMPAD_ENTER to KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_SUBTRACT to KeyEvent.KEYCODE_MINUS,
            KeyEvent.KEYCODE_NUMPAD_EQUALS to KeyEvent.KEYCODE_EQUALS,
            KeyEvent.KEYCODE_NUMPAD_COMMA to KeyEvent.KEYCODE_COMMA,
            KeyEvent.KEYCODE_NUMPAD_DOT to KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_NUMPAD_DIVIDE to KeyEvent.KEYCODE_SLASH,
        ) + (0..9).associate { KeyEvent.KEYCODE_NUMPAD_0 + it to KeyEvent.KEYCODE_0 + it }

    private val MODIFIERS =
        setOf(
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_FUNCTION,
            KeyEvent.KEYCODE_SYM,
        )

    fun codeOf(name: String): Int? = NAMED[name] ?: ALIASES[name]

    fun nameOf(keyCode: Int): String? = NAMES[keyCode]

    fun normalize(keyCode: Int): Int = NUMPAD[keyCode] ?: keyCode

    fun isModifier(keyCode: Int): Boolean = keyCode in MODIFIERS

    private const val FUNCTION_KEYS = 12
}
