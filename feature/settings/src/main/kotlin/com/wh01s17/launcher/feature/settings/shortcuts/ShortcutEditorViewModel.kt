package com.wh01s17.launcher.feature.settings.shortcuts

import androidx.lifecycle.ViewModel
import com.wh01s17.launcher.core.input.binding.BindingTrigger
import com.wh01s17.launcher.core.input.binding.KeyChord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Qué atajo se edita y qué combinación se capturó. */
data class EditorState(
    val trigger: BindingTrigger? = null,
    val captured: KeyChord? = null,
)

/**
 * Estado del editor de atajos. Vive en un ViewModel porque conectar un teclado físico (o rotar)
 * recrea la actividad: en el Redmi, el editor se cerraba y la combinación capturada se perdía.
 */
class ShortcutEditorViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = mutableState.asStateFlow()

    fun open(row: ShortcutRow) {
        mutableState.value = EditorState(row.trigger)
    }

    fun close() {
        mutableState.value = EditorState()
    }

    /** Una tecla con el editor abierto; `false` si está cerrado (la tecla sigue su camino). */
    fun onKey(
        keyCode: Int,
        metaState: Int,
        table: ShortcutTable?,
    ): Boolean {
        val current = state.value
        val trigger = current.trigger ?: return false
        val row = table?.rowFor(trigger) ?: return true
        when (val key = EditorKey.of(keyCode, metaState, row)) {
            EditorKey.Dismiss -> close()
            is EditorKey.Capture -> mutableState.value = current.copy(captured = key.chord)
            EditorKey.Ignore -> Unit
        }
        return true
    }
}

/** La fila de [trigger], o `null` si ya no existe (otra ventana cambió la config). */
fun ShortcutTable.rowFor(trigger: BindingTrigger): ShortcutRow? = (keys + gestures).firstOrNull { it.trigger == trigger }
