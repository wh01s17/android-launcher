package com.wh01s17.launcher.feature.workspaces

import androidx.compose.runtime.Immutable
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.config.GeneralConfig
import com.wh01s17.launcher.core.config.WorkspaceAnimation
import com.wh01s17.launcher.core.tiling.Direction
import com.wh01s17.launcher.core.tiling.Divider
import com.wh01s17.launcher.core.tiling.FloatingBounds
import com.wh01s17.launcher.core.tiling.Gaps
import com.wh01s17.launcher.core.tiling.Rect
import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.core.tiling.Workspace
import com.wh01s17.launcher.feature.workspaces.model.Desk

/** Lo que pinta un tile. */
@Immutable
sealed interface TileInfo {
    /** Una app: si sigue instalada y si se puede abrir ahora. */
    data class App(
        val key: AppKey,
        /** `null` si la app ya no está instalada (o el índice de apps aún no se leyó). */
        val app: AppEntry?,
        /** Perfil de trabajo en pausa o app suspendida: se ve atenuada. */
        val unavailable: Boolean = false,
    ) : TileInfo

    /** Un widget del sistema: lo pinta el [com.wh01s17.launcher.feature.workspaces.ui.WidgetRenderer] de la app. */
    data class Widget(
        val appWidgetId: Int,
    ) : TileInfo

    /** Un widget propio de wh01s17; también lo pinta el renderer de la app. */
    data class Native(
        val id: String,
    ) : TileInfo
}

enum class WorkspaceMessage { NotInstalled, ProfilePaused, ProfileResumeRequested, LaunchFailed }

@Immutable
data class WorkspacesUiState(
    /** `false` hasta leer los escritorios guardados. */
    val loaded: Boolean = false,
    val current: Int = Desk.FIRST,
    val workspaces: List<Workspace> = emptyList(),
    val tiles: Map<TileId, TileInfo> = emptyMap(),
    /** El índice de apps ya se leyó: un tile sin app es una app desinstalada, no una que carga. */
    val appsLoaded: Boolean = false,
    val editing: Boolean = false,
    val general: GeneralConfig = GeneralConfig(),
    val animation: WorkspaceAnimation = WorkspaceAnimation(),
    val message: WorkspaceMessage? = null,
)

/**
 * Medidas del lienzo en píxeles. Sólo las conoce la UI y hacen falta para el foco direccional, el
 * intercambio y el tamaño, que dependen de dónde cae cada tile.
 */
data class CanvasMetrics(
    val area: Rect,
    val gaps: Gaps,
    val density: Float,
)

/** Lo que hace el usuario sobre los escritorios (UDF, plan.md §5.1). */
sealed interface WorkspacesIntent {
    /** El pager se paró en [workspace] tras deslizar. */
    data class PageSettled(
        val workspace: Int,
    ) : WorkspacesIntent

    /** Tocar un tile: lo enfoca y, fuera del modo edición, abre su app. */
    data class TapTile(
        val tile: TileId,
        val origin: LaunchOrigin? = null,
    ) : WorkspacesIntent

    /** Tocar una pestaña de un grupo: la muestra y la enfoca, sin abrir la app. */
    data class SelectTab(
        val tile: TileId,
    ) : WorkspacesIntent

    /** Mantener pulsado un tile: modo edición con ese tile enfocado. */
    data class LongPressTile(
        val tile: TileId,
    ) : WorkspacesIntent

    /** Mantener pulsado un hueco vacío: modo edición (plan.md §7 Hito 4). */
    data object LongPressBackground : WorkspacesIntent

    data object ExitEdit : WorkspacesIntent

    /**
     * Soltar un tile cerca del borde de otro: queda a ese lado ([side]) y el destino no se mueve,
     * como al arrastrar una ventana en Hyprland (no es un intercambio).
     */
    data class MoveNextTo(
        val dragged: TileId,
        val target: TileId,
        val side: Direction,
    ) : WorkspacesIntent

    /** Soltar un tile sobre el centro de otro: se agrupan (el arrastrado entra como pestaña). */
    data class GroupTiles(
        val dragged: TileId,
        val target: TileId,
    ) : WorkspacesIntent

    /** Soltar un tile sobre el número de otro escritorio: se va allí sin seguirlo. */
    data class DropOnWorkspace(
        val tile: TileId,
        val workspace: Int,
    ) : WorkspacesIntent

    /** Arrastrar un borde compartido hasta [positionPx] (ver `moveDivider`). */
    data class DragDivider(
        val divider: Divider,
        val positionPx: Int,
    ) : WorkspacesIntent

    /** Mover o redimensionar un tile flotante. */
    data class MoveFloating(
        val tile: TileId,
        val bounds: FloatingBounds,
    ) : WorkspacesIntent

    data class CanvasChanged(
        val metrics: CanvasMetrics,
    ) : WorkspacesIntent

    /** El aviso ya se mostró. */
    data object MessageShown : WorkspacesIntent
}
