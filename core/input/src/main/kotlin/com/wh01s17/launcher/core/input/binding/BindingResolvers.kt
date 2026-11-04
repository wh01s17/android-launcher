package com.wh01s17.launcher.core.input.binding

import com.wh01s17.launcher.core.input.LauncherCommand

/**
 * Teclado físico → comando (plan.md §5.7), según los `bindings` de combinaciones de teclas. Los
 * modificadores tienen que coincidir exactamente: `SUPER + 1` no salta con `SUPER + SHIFT + 1`.
 */
class KeyBindingResolver(
    bindings: List<Binding>,
) {
    private val byChord: Map<KeyChord, LauncherCommand> =
        bindings.mapNotNull { binding -> (binding.trigger as? BindingTrigger.Keys)?.let { it.chord to binding.command } }.toMap()

    /**
     * El comando de una pulsación (`KeyEvent.keyCode` y `metaState`), o `null`. Con [typing] (hay un
     * campo de texto con foco) se ignoran los atajos sin `SUPER`, `CTRL` ni `ALT`: `SHIFT + 1` es
     * escribir «!».
     */
    fun resolve(
        keyCode: Int,
        metaState: Int,
        typing: Boolean = false,
    ): LauncherCommand? {
        val chord = KeyChord.fromEvent(keyCode, metaState) ?: return null
        if (typing && chord.modifiers.all { it == KeyModifier.Shift }) return null
        return byChord[chord]
    }
}

/** Gesto táctil reconocido por la UI → comando, según los `bindings` de gestos. */
class GestureBindingResolver(
    bindings: List<Binding>,
) {
    private val byGesture: Map<Gesture, LauncherCommand> =
        bindings.mapNotNull { binding -> (binding.trigger as? BindingTrigger.Touch)?.let { it.gesture to binding.command } }.toMap()

    fun resolve(gesture: Gesture): LauncherCommand? = byGesture[gesture]
}
