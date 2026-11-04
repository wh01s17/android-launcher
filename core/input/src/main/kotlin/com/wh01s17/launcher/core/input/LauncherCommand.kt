package com.wh01s17.launcher.core.input

/**
 * Acciones del launcher (plan.md §5.3). Teclado, gestos, barra y menú emiten comandos y nadie
 * llama a los ViewModels ajenos directamente. La lista crece con cada hito.
 */
sealed interface LauncherCommand {
    /** `Super + Space`: abrir el menú y la paleta unificados (Hito 5); si ya está en la raíz, cerrarlo. */
    data object OpenMenu : LauncherCommand

    /** `Super + Alt + Space`: el menú en la página de apps; si ya está ahí, cerrarlo. */
    data object OpenAppsMenu : LauncherCommand

    /** `Super + Escape`: el menú en la página de sistema; si ya está ahí, cerrarlo. */
    data object OpenSystemMenu : LauncherCommand

    /** `Ctrl + Alt + A/Z`: sube ([steps] > 0) o baja el volumen multimedia, con el panel del sistema. */
    data class AdjustVolume(
        val steps: Int,
    ) : LauncherCommand

    /** Abrir la cortina de notificaciones del sistema (necesita el servicio de accesibilidad). */
    data object OpenNotificationShade : LauncherCommand
}

/** Hacia dónde va un comando de foco, intercambio o tamaño (las flechas de `bindings/tiling.lua`). */
enum class CommandDirection { Left, Right, Up, Down }

/**
 * Comandos de los workspaces y sus tiles (plan.md §3, `bindings/tiling.lua`). Los atiende
 * `feature/workspaces`; los que actúan sobre "el tile" usan el que tiene el foco en el workspace
 * actual y no hacen nada si no hay ninguno.
 */
sealed interface WorkspaceCommand : LauncherCommand {
    /** `Super + 1…0`: ir al workspace [id] (1–10). */
    data class FocusWorkspace(
        val id: Int,
    ) : WorkspaceCommand

    /** `Super + Tab` / `Super + Shift + Tab`: el siguiente o anterior con tiles (`e+1`, `e-1`). */
    data class CycleWorkspace(
        val forward: Boolean,
    ) : WorkspaceCommand

    /** `Super + Shift + 1…0`: llevar el tile al workspace [workspace]; con [follow], ir con él. */
    data class MoveTileToWorkspace(
        val workspace: Int,
        val follow: Boolean = true,
    ) : WorkspaceCommand

    /** `Super + flechas`. */
    data class MoveFocus(
        val direction: CommandDirection,
    ) : WorkspaceCommand

    /** `Super + Shift + flechas`. */
    data class SwapTile(
        val direction: CommandDirection,
    ) : WorkspaceCommand

    /** `Super + -`/`=` (`resizeactive`): mueve el borde derecho [dx] y el inferior [dy], en dp. */
    data class ResizeTile(
        val dx: Int,
        val dy: Int,
    ) : WorkspaceCommand

    /** `Super + T`. */
    data object ToggleFloating : WorkspaceCommand

    /** `Super + J`. */
    data object ToggleSplit : WorkspaceCommand

    /** `Super + F`. */
    data object ToggleFullscreen : WorkspaceCommand

    /** `Super + L`: dwindle ↔ master en el workspace actual. */
    data object ToggleLayout : WorkspaceCommand

    /** `Super + W`: quita el tile (nunca cierra la app, plan.md §2.2). */
    data object CloseTile : WorkspaceCommand

    /** Siguiente o anterior pestaña del grupo del tile. */
    data class CycleGroup(
        val forward: Boolean,
    ) : WorkspaceCommand

    /** Entrar o salir del modo edición (mantener pulsado un hueco vacío). */
    data object ToggleEditMode : WorkspaceCommand
}
