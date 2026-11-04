package com.wh01s17.launcher.core.designsystem.palette

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Los colores de un reproductor con la forma de las paletas de tidalamp (`colors.toml` de Omarchy):
 * fondo, banda (la fila de transporte, `lighter_background`), texto, texto apagado
 * (`dark_foreground`, o `foreground` si la paleta no lo trae, como hace tidalamp), reglas y acento.
 */
@Immutable
data class PlayerPalette(
    val name: String,
    val background: Color,
    val band: Color,
    val foreground: Color,
    val dim: Color,
    val muted: Color,
    val accent: Color,
)

/**
 * Las paletas incluidas en tidalamp (`tidalamp/theme.py`, `_BUILTIN_SOURCES`), para el widget de
 * TIDAL. Aquí y no en el widget por la regla R6 (los colores sólo salen del sistema de diseño).
 * `phosphor` es la de wh01s17 y va primero: es la de fábrica.
 */
@Suppress("MagicNumber") // Datos copiados tal cual de tidalamp.
object TidalampPalettes {
    val all: List<PlayerPalette> =
        listOf(
            PlayerPalette(
                "phosphor",
                background = Color(0xFF0A0E0F),
                band = Color(0xFF171E23),
                foreground = Color(0xFFCBD5CE),
                dim = Color(0xFF748178),
                muted = Color(0xFF33454F),
                accent = Color(0xFF84C959),
            ),
            PlayerPalette(
                "black",
                background = Color(0xFF000000),
                band = Color(0xFF141414),
                foreground = Color(0xFFE6E6E6),
                dim = Color(0xFF8A8A8A),
                muted = Color(0xFF3A3A3A),
                accent = Color(0xFFF5F5F5),
            ),
            PlayerPalette(
                "tokyo-night",
                background = Color(0xFF1A1B26),
                band = Color(0xFF24283B),
                foreground = Color(0xFFC0CAF5),
                dim = Color(0xFFC0CAF5),
                muted = Color(0xFF565F89),
                accent = Color(0xFF7AA2F7),
            ),
            PlayerPalette(
                "catppuccin",
                background = Color(0xFF1E1E2E),
                band = Color(0xFF313244),
                foreground = Color(0xFFCDD6F4),
                dim = Color(0xFFCDD6F4),
                muted = Color(0xFF6C7086),
                accent = Color(0xFFCBA6F7),
            ),
            PlayerPalette(
                "nord",
                background = Color(0xFF2E3440),
                band = Color(0xFF3B4252),
                foreground = Color(0xFFD8DEE9),
                dim = Color(0xFFD8DEE9),
                muted = Color(0xFF4C566A),
                accent = Color(0xFF88C0D0),
            ),
            PlayerPalette(
                "gruvbox",
                background = Color(0xFF282828),
                band = Color(0xFF3C3836),
                foreground = Color(0xFFEBDBB2),
                dim = Color(0xFFEBDBB2),
                muted = Color(0xFF928374),
                accent = Color(0xFFD79921),
            ),
            PlayerPalette(
                "unidad-morada",
                background = Color(0xFF1C1128),
                band = Color(0xFF2E1D44),
                foreground = Color(0xFFE6DCF7),
                dim = Color(0xFFA896C4),
                muted = Color(0xFF6A4F8A),
                accent = Color(0xFF9BFF4D),
            ),
            PlayerPalette(
                "pirata",
                background = Color(0xFF0E2A3F),
                band = Color(0xFF173E5C),
                foreground = Color(0xFFF4ECD8),
                dim = Color(0xFFA9BCCB),
                muted = Color(0xFF4A7391),
                accent = Color(0xFFFFD23F),
            ),
            PlayerPalette(
                "cuaderno",
                background = Color(0xFF0B0B0B),
                band = Color(0xFF1A1A1A),
                foreground = Color(0xFFEDEDED),
                dim = Color(0xFF8A8A8A),
                muted = Color(0xFF444444),
                accent = Color(0xFFE5383B),
            ),
            PlayerPalette(
                "neon-noir",
                background = Color(0xFF0B0B14),
                band = Color(0xFF1A1A2E),
                foreground = Color(0xFFE4F6F8),
                dim = Color(0xFF7FA3AA),
                muted = Color(0xFF3A3F5C),
                accent = Color(0xFFFCEE0A),
            ),
            PlayerPalette(
                "runas",
                background = Color(0xFF15170F),
                band = Color(0xFF27291C),
                foreground = Color(0xFFE8DCC0),
                dim = Color(0xFFA89D80),
                muted = Color(0xFF5C5540),
                accent = Color(0xFFD4A93A),
            ),
            PlayerPalette(
                "reggae",
                background = Color(0xFF10100A),
                band = Color(0xFF1F1F14),
                foreground = Color(0xFFF2EFE0),
                dim = Color(0xFFAAA68C),
                muted = Color(0xFF4F4A2A),
                accent = Color(0xFFF7D117),
            ),
            PlayerPalette(
                "comodin",
                background = Color(0xFF150E1D),
                band = Color(0xFF2A1A3A),
                foreground = Color(0xFFEBE4D4),
                dim = Color(0xFFA99BB8),
                muted = Color(0xFF5A3F73),
                accent = Color(0xFFB77CFF),
            ),
            PlayerPalette(
                "gotico",
                background = Color(0xFF0E0A0F),
                band = Color(0xFF1E1522),
                foreground = Color(0xFFDCD2DE),
                dim = Color(0xFF9A8C9E),
                muted = Color(0xFF4A3A4D),
                accent = Color(0xFFD63A5C),
            ),
            PlayerPalette(
                "death-metal",
                background = Color(0xFF070707),
                band = Color(0xFF171513),
                foreground = Color(0xFFDDD6C6),
                dim = Color(0xFF8F887A),
                muted = Color(0xFF3D3A35),
                accent = Color(0xFFE0102A),
            ),
            PlayerPalette(
                "bosque",
                background = Color(0xFF0F1A12),
                band = Color(0xFF1B2C1E),
                foreground = Color(0xFFE4ECD6),
                dim = Color(0xFF9BAB8E),
                muted = Color(0xFF3F5A3A),
                accent = Color(0xFF86CC5A),
            ),
        )

    val default: PlayerPalette get() = all.first()

    fun byName(name: String?): PlayerPalette = all.firstOrNull { it.name == name } ?: default

    /** La siguiente de la lista, para cambiar con un toque. */
    fun after(palette: PlayerPalette): PlayerPalette = all[(all.indexOf(palette) + 1) % all.size]
}
