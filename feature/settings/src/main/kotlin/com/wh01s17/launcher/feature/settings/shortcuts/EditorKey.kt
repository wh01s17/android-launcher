package com.wh01s17.launcher.feature.settings.shortcuts

import android.view.KeyEvent
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.core.input.binding.KeyChord

/**
 * Lo que hace una tecla con el editor abierto: Escape sin modificadores cierra; en un atajo de
 * teclado, cualquier otra combinación (no un modificador solo) pasa a ser la capturada.
 */
sealed interface EditorKey {
    data object Dismiss : EditorKey

    data class Capture(
        val chord: KeyChord,
    ) : EditorKey

    data object Ignore : EditorKey

    companion object {
        fun of(
            keyCode: Int,
            metaState: Int,
            row: ShortcutRow,
        ): EditorKey {
            val chord = KeyChord.fromEvent(keyCode, metaState) ?: return Ignore
            return when {
                chord.modifiers.isEmpty() && keyCode == KeyEvent.KEYCODE_ESCAPE -> Dismiss
                row.trigger is BindingTrigger.Keys -> Capture(chord)
                else -> Ignore
            }
        }
    }
}
