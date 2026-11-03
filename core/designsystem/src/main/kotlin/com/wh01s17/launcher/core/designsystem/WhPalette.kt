package com.wh01s17.launcher.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Paleta mínima provisional del tema wh01s17 (Hito 0).
 *
 * En el Hito 1 se reemplaza por `WhTokens`, generado desde los TOML de
 * `~/dotfiles` (plan.md §4.1). Fuera de este módulo no se escriben colores
 * literales (plan.md §1 R6).
 */
object WhPalette {
    /** `background` de colors.toml: superficie base del launcher. */
    val Void = Color(0xFF0A0E0F)
}
