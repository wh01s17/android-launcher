package com.wh01s17.launcher.core.designsystem.widgets

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * La temática de los widgets propios de wh01s17 (`appearance.widgetTheme`, se elige en Estilo). Cada
 * temática pinta los mismos widgets con los mismos datos: cambiarla no toca los escritorios. Para
 * añadir una: una entrada aquí, su piel en `feature/widgets` (`WidgetSkin`) y su nombre en Estilo.
 */
enum class WidgetTheme(
    val id: String,
) {
    /** El tema *nova* de tidalamp: encabezado con barra de acento, cifras grandes y bandas de teclas. */
    Nova("nova"),

    /** Terminal: prompt, salida de comandos, barras ASCII y teclas `[run]` en vídeo inverso. */
    Hack("hack"),
    ;

    companion object {
        val Default = Nova

        /** Una temática desconocida (de una versión futura, o mal escrita) se pinta con la de fábrica. */
        fun byId(id: String?): WidgetTheme = entries.firstOrNull { it.id == id } ?: Default
    }
}

/** La temática con la que se pintan los widgets propios; la pone la app desde la config. */
val LocalWidgetTheme = staticCompositionLocalOf { WidgetTheme.Default }
