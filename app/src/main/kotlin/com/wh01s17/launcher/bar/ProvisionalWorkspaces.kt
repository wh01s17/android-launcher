package com.wh01s17.launcher.bar

import com.wh01s17.launcher.feature.bar.WorkspaceBarState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Escritorios provisionales para la barra, hasta que el motor de tiling y los workspaces (Hito 4)
 * den los reales. Sólo recuerda cuál está "activo"; no hay tiles ni escritorios detrás.
 */
class ProvisionalWorkspaces {
    private val mutableState = MutableStateFlow(WorkspaceBarState())
    val state: StateFlow<WorkspaceBarState> = mutableState.asStateFlow()

    fun focus(id: Int) = mutableState.update { it.copy(focused = id.coerceIn(WorkspaceBarState.FIRST, WorkspaceBarState.LAST)) }
}
