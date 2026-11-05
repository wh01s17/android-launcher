package com.wh01s17.launcher.core.designsystem.palette

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Quien sabe qué paleta eligió el usuario fuera de Compose (la `Application`): la usan las vistas
 * clásicas, como el salvapantallas de Android, que no están bajo `WhTheme`. Es un flujo porque, con el
 * proceso recién arrancado, la config todavía se está cargando.
 */
interface PaletteHost {
    val palettes: Flow<PlayerPalette>
}

/** Los acentos de la paleta del [PaletteHost] de [application], o el de fábrica si no hay. */
fun accentsOf(application: Any?): Flow<Color> =
    ((application as? PaletteHost)?.palettes ?: flowOf(TidalampPalettes.default))
        .map { it.accent }
        .distinctUntilChanged()
