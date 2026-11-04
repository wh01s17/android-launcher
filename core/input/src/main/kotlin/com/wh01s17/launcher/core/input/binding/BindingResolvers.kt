package com.wh01s17.launcher.core.input.binding

import android.view.KeyEvent
import com.wh01s17.launcher.core.input.LauncherCommand

/**
 * Teclado físico → comando (plan.md §5.7), según los `bindings` de combinaciones de teclas. Los
 * modificadores tienen que coincidir exactamente: `SUPER + 1` no salta con `SUPER + SHIFT + 1`.
 *
 * [superAlternative] hace de `SUPER` cuando Android no entrega la tecla Meta (docs/bindings.md): con
 * `ALT`, `ALT + 1` dispara `SUPER + 1`. Primero se busca la combinación tal cual, así un atajo propio
 * con ese modificador (`CTRL + ALT + A`) sigue ganando.
 */
class KeyBindingResolver(
    bindings: List<Binding>,
    private val superAlternative: KeyModifier? = null,
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
        return byChord[chord] ?: asSuper(chord)?.let(byChord::get)
    }

    /**
     * Lo que hay que pulsar en este equipo para disparar [chord], o `null` si no se puede (detección
     * de reservados, plan.md §7 Hito 6). Con [superDelivered] en `false` (lo normal en Android 15),
     * `SUPER` se sustituye por [superAlternative]; la combinación resultante no puede estar reservada
     * por el sistema ni tapada por otro atajo que coincida tal cual.
     */
    fun pressFor(
        chord: KeyChord,
        superDelivered: Boolean = false,
    ): KeyChord? {
        val alternative = superAlternative
        val press =
            when {
                KeyModifier.Super !in chord.modifiers || superDelivered -> chord
                alternative == null || alternative in chord.modifiers -> return null
                else -> chord.copy(modifiers = chord.modifiers - KeyModifier.Super + alternative)
            }
        val shadowed = press != chord && press in byChord
        return press.takeIf { !shadowed && SystemReservedChords.reason(press, superDelivered) == null }
    }

    private fun asSuper(chord: KeyChord): KeyChord? {
        val alternative = superAlternative ?: return null
        if (alternative !in chord.modifiers || KeyModifier.Super in chord.modifiers) return null
        return chord.copy(modifiers = chord.modifiers - alternative + KeyModifier.Super)
    }
}

/**
 * Combinaciones que Android se queda antes de que lleguen al launcher. Sólo las comprobadas
 * (docs/bindings.md, Redmi con HyperOS 2 / Android 15 y emulador API 35, con `input keycombination`).
 */
object SystemReservedChords {
    enum class Reason {
        /** Ningún evento con Meta llega a la app (ni la tecla sola). */
        SuperNotDelivered,

        /** `ALT + TAB` abre Recientes. */
        Recents,
    }

    private val RECENTS = KeyChord(KeyEvent.KEYCODE_TAB, setOf(KeyModifier.Alt))

    fun reason(
        chord: KeyChord,
        superDelivered: Boolean = false,
    ): Reason? =
        when {
            KeyModifier.Super in chord.modifiers && !superDelivered -> Reason.SuperNotDelivered
            chord == RECENTS -> Reason.Recents
            else -> null
        }
}

/** Gesto táctil reconocido por la UI → comando, según los `bindings` de gestos. */
class GestureBindingResolver(
    bindings: List<Binding>,
) {
    private val byGesture: Map<Gesture, LauncherCommand> =
        bindings.mapNotNull { binding -> (binding.trigger as? BindingTrigger.Touch)?.let { it.gesture to binding.command } }.toMap()

    /** Los gestos con atajo: la UI sólo espera un doble toque si está aquí. */
    val bound: Set<Gesture> get() = byGesture.keys

    fun resolve(gesture: Gesture): LauncherCommand? = byGesture[gesture]
}
