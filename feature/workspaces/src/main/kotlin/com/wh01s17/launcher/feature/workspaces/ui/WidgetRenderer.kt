package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Quien pinta los widgets dentro de los tiles. Lo pone la app con [LocalWidgetRenderer] (la
 * implementación está en `feature/widgets`): un feature no depende de otro (plan.md §5.2).
 */
interface WidgetRenderer {
    /**
     * Pinta el widget del sistema [appWidgetId]. Con [interactive] recibe sus toques y mantenerlo
     * pulsado llama a [onLongPress]; sin él (modo edición) deja pasar los toques para arrastrar el tile.
     */
    @Composable
    fun Content(
        appWidgetId: Int,
        interactive: Boolean,
        onLongPress: () -> Unit,
        modifier: Modifier,
    )

    /** Pinta el widget propio [id]; sin [interactive] (modo edición) sus botones no responden. */
    @Composable
    fun Native(
        id: String,
        interactive: Boolean,
        modifier: Modifier,
    )
}

/** Sin renderer (pruebas, capturas), un widget se ve como un tile vacío con su nombre genérico. */
val LocalWidgetRenderer = staticCompositionLocalOf<WidgetRenderer?> { null }
