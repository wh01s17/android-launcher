package com.wh01s17.launcher.core.input.binding

import com.wh01s17.launcher.core.input.LauncherCommand

/**
 * Gestos táctiles que se pueden asignar en `bindings` (plan.md §3). Deslizar a izquierda o derecha
 * cambia de escritorio siempre (lo hace el pager) y no se asigna.
 */
enum class Gesture(
    val configName: String,
) {
    /** Un dedo hacia arriba sobre el escritorio. */
    SwipeUp("swipe-up"),

    /** Un dedo hacia abajo sobre el escritorio. */
    SwipeDown("swipe-down"),
    ;

    companion object {
        fun fromConfig(name: String): Gesture? = entries.firstOrNull { it.configName == name }
    }
}

/** Lo que dispara un atajo: una combinación de teclas o un gesto. */
sealed interface BindingTrigger {
    data class Keys(
        val chord: KeyChord,
    ) : BindingTrigger {
        override fun toString() = chord.toString()
    }

    data class Touch(
        val gesture: Gesture,
    ) : BindingTrigger {
        override fun toString() = gesture.configName
    }
}

/** Una entrada de `bindings` (plan.md §6.4): disparador → comando. */
data class Binding(
    val trigger: BindingTrigger,
    val command: LauncherCommand,
)
