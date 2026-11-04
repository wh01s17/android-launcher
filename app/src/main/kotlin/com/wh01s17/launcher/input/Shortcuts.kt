package com.wh01s17.launcher.input

import android.view.KeyEvent
import com.wh01s17.launcher.core.config.ConfigState
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.input.binding.Binding
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
    private var source: List<Binding>? = null
    private var keys = KeyBindingResolver(emptyList())
    private var gestures = GestureBindingResolver(emptyList())

    /** La tecla cuyo `ACTION_DOWN` fue un atajo: su `ACTION_UP` también se consume. */
    private var heldKey: Int? = null

    /**
     * `true` si [event] es (o termina) un atajo y no debe seguir hacia la UI. Mantener la tecla sólo
     * repite los cambios de tamaño, como `binde` en Hyprland; las demás repeticiones se consumen.
     */
    fun onKeyEvent(
        event: KeyEvent,
        typing: Boolean,
    ): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                refresh()
                val command = keys.resolve(event.keyCode, event.metaState, typing) ?: return false
                heldKey = event.keyCode
                if (event.repeatCount == 0 || command is WorkspaceCommand.ResizeTile) send(command)
                return true
            }
            KeyEvent.ACTION_UP ->
                if (event.keyCode == heldKey) {
                    heldKey = null
                    return true
                }
        }
        return false
    }

    fun onGesture(gesture: Gesture) {
        refresh()
        gestures.resolve(gesture)?.let(send)
    }

    private fun refresh() {
        val bindings = config.value.config?.bindings ?: return
        if (bindings === source) return
        source = bindings
        keys = KeyBindingResolver(bindings)
        gestures = GestureBindingResolver(bindings)
    }
}
