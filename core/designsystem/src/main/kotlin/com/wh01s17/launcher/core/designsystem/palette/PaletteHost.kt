package com.wh01s17.launcher.core.designsystem.palette

/**
 * Quien sabe qué paleta eligió el usuario fuera de Compose (la `Application`): la usan las vistas
 * clásicas, como el salvapantallas de Android, que no están bajo `WhTheme`.
 */
interface PaletteHost {
    val palette: PlayerPalette
}

/** La paleta del [PaletteHost] de [application], o la de fábrica si no hay. */
fun paletteOf(application: Any?): PlayerPalette = (application as? PaletteHost)?.palette ?: TidalampPalettes.default
