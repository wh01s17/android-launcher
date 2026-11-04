package com.wh01s17.launcher.core.input

/**
 * Acciones del launcher (plan.md §5.3). Teclado, gestos, barra y menú emiten comandos y nadie
 * llama a los ViewModels ajenos directamente. La lista crece con cada hito; hoy sólo están las que
 * usa la barra.
 */
sealed interface LauncherCommand {
    /** Abrir el menú (por ahora, el drawer provisional de apps; el menú wh01s17 llega en el Hito 5). */
    data object OpenMenu : LauncherCommand

    /** Ir al workspace [id] (1–10). */
    data class FocusWorkspace(
        val id: Int,
    ) : LauncherCommand

    /** Abrir la cortina de notificaciones del sistema (necesita el servicio de accesibilidad). */
    data object OpenNotificationShade : LauncherCommand
}
