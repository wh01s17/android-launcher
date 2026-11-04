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
     * Qué hay que pulsar en este equipo para disparar [chord] (detección de reservados, plan.md §7
     * Hito 6). Con [superDelivered] en `false` (lo normal en Android 15), `SUPER` se sustituye por
     * [superAlternative]; la combinación resultante no puede estar reservada por el sistema ni tapada
     * por otro atajo que coincida tal cual.
     */
    fun availability(
        chord: KeyChord,
        superDelivered: Boolean = false,
    ): Availability {
        val alternative = superAlternative
        val press =
            when {
                KeyModifier.Super !in chord.modifiers || superDelivered -> chord
                alternative == null -> return Availability.Blocked(BlockReason.NoSuper)
                alternative in chord.modifiers -> return Availability.Blocked(BlockReason.AlternativeTwice)
                else -> chord.copy(modifiers = chord.modifiers - KeyModifier.Super + alternative)
            }
        return when {
            press != chord && press in byChord -> Availability.Blocked(BlockReason.Shadowed)
            SystemReservedChords.reason(press, superDelivered) == SystemReservedChords.Reason.Recents ->
                Availability.Blocked(BlockReason.Recents)
            else -> Availability.Press(press)
        }
    }

    /** Lo que hay que pulsar, o `null` si no se puede (ver [availability]). */
    fun pressFor(
        chord: KeyChord,
        superDelivered: Boolean = false,
    ): KeyChord? = (availability(chord, superDelivered) as? Availability.Press)?.chord

    private fun asSuper(chord: KeyChord): KeyChord? {
        val alternative = superAlternative ?: return null
        if (alternative !in chord.modifiers || KeyModifier.Super in chord.modifiers) return null
        return chord.copy(modifiers = chord.modifiers - alternative + KeyModifier.Super)
    }
}

sealed interface Availability {
    data class Press(
        val chord: KeyChord,
    ) : Availability

    data class Blocked(
        val reason: BlockReason,
    ) : Availability
}

/** Por qué no se puede pulsar un atajo. */
enum class BlockReason {
    /** Lleva `SUPER`, Android no lo entrega y no hay modificador alternativo. */
    NoSuper,

    /** Ya lleva el modificador alternativo (`SUPER + ALT + …` con `ALT`). */
    AlternativeTwice,

    /** Con el alternativo coincide con otro atajo, que gana. */
    Shadowed,

    /** `ALT + TAB`: Android abre Recientes. */
    Recents,
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
