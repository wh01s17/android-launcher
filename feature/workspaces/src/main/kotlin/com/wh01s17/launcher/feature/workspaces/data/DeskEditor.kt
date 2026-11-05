package com.wh01s17.launcher.feature.workspaces.data

import com.wh01s17.launcher.feature.workspaces.model.Desk
import com.wh01s17.launcher.feature.workspaces.model.UndoHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Los cambios de los escritorios con «deshacer»: cada uno se aplica a [desk] y, si mueve algo de
 * la distribución, se guarda en un [UndoHistory]. [undoSteps] dice cuántos pasos quedan.
 */
internal class DeskEditor(
    private val desk: DeskRepository,
    private val history: UndoHistory = UndoHistory(),
) {
    private val steps = MutableStateFlow(0)
    val undoSteps: StateFlow<Int> = steps.asStateFlow()

    /** Aplica [transform]; los pasos seguidos con la misma [key] (un arrastre) se deshacen juntos. */
    fun change(
        key: String? = null,
        transform: (Desk) -> Desk,
    ) {
        val before = desk.state.value
        desk.update(transform)
        val after = desk.state.value
        if (before != null && after != null) history.record(before, after, key)
        steps.value = history.size
    }

    /** Vuelve al estado anterior al último cambio, sin moverse del escritorio en el que se está. */
    fun undo() {
        val previous = history.pop() ?: return
        desk.update { now -> previous.focusWorkspace(now.current) }
        steps.value = history.size
    }
}
