package com.wh01s17.launcher.input

import android.view.KeyEvent
import com.wh01s17.launcher.core.config.ConfigState
import com.wh01s17.launcher.core.config.LauncherConfig
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.input.binding.Gesture
import com.wh01s17.launcher.core.input.binding.GestureBindingResolver
import com.wh01s17.launcher.core.input.binding.KeyBindingResolver
import kotlinx.coroutines.flow.StateFlow

/**
 * Atajos de teclado y gestos de la config vigente → [send] (el bus de comandos, plan.md §5.7). Los
 * resolvedores se rehacen sólo cuando cambian los `bindings`. Sólo se usa en el hilo principal.
 */
internal class Shortcuts(
    private val config: StateFlow<ConfigState>,
    private val send: (LauncherCommand) -> Unit,
) {
    private var source: LauncherConfig? = null
    private var keys = KeyBindingResolver(emptyList())
    private var gestures = GestureBindingResolver(emptyList())

    /** La tecla cuyo `ACTION_DOWN` fue un atajo: su `ACTION_UP` también se consume. */
    private var heldKey: Int? = null

    /**
     * Si alguna vez llegó un evento con Meta en este proceso. En Android 15 (Redmi y emulador) no
     * llega ninguno; un teclado o un sistema que sí lo entregue hace válidos los atajos con `SUPER`.
     */
    var superDelivered: Boolean = false
        private set

    /**
     * `true` si [event] es (o termina) un atajo y no debe seguir hacia la UI. Mantener la tecla sólo
     * repite los cambios de tamaño y de volumen, como `binde` en Hyprland; las demás repeticiones se
     * consumen.
     */
    fun onKeyEvent(
        event: KeyEvent,
        typing: Boolean,
    ): Boolean {
        if (event.metaState and KeyEvent.META_META_ON != 0) superDelivered = true
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> onKeyDown(event, typing)
            KeyEvent.ACTION_UP -> (event.keyCode == heldKey).also { if (it) heldKey = null }
            else -> false
        }
    }

    private fun onKeyDown(
        event: KeyEvent,
        typing: Boolean,
    ): Boolean {
        refresh()
        val command = keys.resolve(event.keyCode, event.metaState, typing) ?: return false
        heldKey = event.keyCode
        if (event.repeatCount == 0 || command.repeats) send(command)
        return true
    }

    fun onGesture(gesture: Gesture) {
        refresh()
        gestures.resolve(gesture)?.let(send)
    }

    private fun refresh() {
        val current = config.value.config ?: return
        if (current === source) return
        source = current
        keys = KeyBindingResolver(current.bindings, current.keyboard.superAlternative)
        gestures = GestureBindingResolver(current.bindings)
    }

    companion object {
        /** Los gestos con atajo en [config]; la UI sólo espera un doble toque si está asignado. */
        fun boundGestures(config: LauncherConfig?): Set<Gesture> = GestureBindingResolver(config?.bindings.orEmpty()).bound
    }
}

private val LauncherCommand.repeats: Boolean
    get() = this is WorkspaceCommand.ResizeTile || this is LauncherCommand.AdjustVolume
