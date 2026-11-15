package com.wh01s17.launcher.core.designsystem.screensaver

import androidx.compose.ui.graphics.Color
import com.wh01s17.launcher.core.designsystem.palette.PaletteRemap
import com.wh01s17.launcher.core.designsystem.palette.PlayerPalette
import com.wh01s17.launcher.core.designsystem.tokens.WhTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

/** Lo principal del salvapantallas. */
enum class ScreensaverStyle(
    val id: String,
) {
    /** La W de puntos de `w-spin.py`, girando. */
    W("w"),

    /** La hora en grande; los dos puntos parpadean. */
    Clock("clock"),

    /** Un texto propio en grande, con un cursor de terminal. */
    Text("text"),

    /** Nada grande: sólo las líneas elegidas, lo mínimo encendido. */
    Minimal("minimal"),
    ;

    companion object {
        fun byId(id: String?): ScreensaverStyle = entries.firstOrNull { it.id == id } ?: W
    }
}

/** El color del salvapantallas: un papel de la paleta elegida (los avisos, fijos como en el tema). */
enum class ScreensaverColor(
    val id: String,
) {
    Accent("accent"),
    Cyan("cyan"),
    Foreground("foreground"),
    Red("red"),
    Yellow("yellow"),
    Blue("blue"),
    Magenta("magenta"),
    ;

    fun of(palette: PlayerPalette): Color =
        when (this) {
            Accent -> palette.accent
            Cyan -> PaletteRemap.secondary(palette)
            Foreground -> palette.foreground
            Red -> WhTokens.Colors.red
            Yellow -> WhTokens.Colors.yellow
            Blue -> WhTokens.Colors.blue
            Magenta -> WhTokens.Colors.magenta
        }

    companion object {
        fun byId(id: String?): ScreensaverColor = entries.firstOrNull { it.id == id } ?: Accent
    }
}

/**
 * Cómo se ve el salvapantallas (`screensaver` de la config). [brightness], [speed] y [size] son
 * fracciones (1 = 100 %). El fondo es siempre negro puro y la deriva anti-quemado no se apaga.
 */
data class ScreensaverSettings(
    val style: ScreensaverStyle = ScreensaverStyle.W,
    val text: String = "wh01s17",
    val showTime: Boolean = false,
    val showDate: Boolean = false,
    val showBattery: Boolean = false,
    val showText: Boolean = false,
    /** El texto del estilo [ScreensaverStyle.Text] gira sobre su eje vertical, como la W. */
    val spinText: Boolean = false,
    val color: ScreensaverColor = ScreensaverColor.Accent,
    val brightness: Float = 1f,
    val speed: Float = 1f,
    val size: Float = 1f,
)

/** Quien sabe qué salvapantallas eligió el usuario fuera de Compose (la `Application`), como [PaletteHost]. */
interface ScreensaverHost {
    val screensaverSettings: Flow<ScreensaverSettings>
}

/** Los ajustes del [ScreensaverHost] de [application], o los de fábrica si no hay. */
fun screensaverSettingsOf(application: Any?): Flow<ScreensaverSettings> =
    ((application as? ScreensaverHost)?.screensaverSettings ?: flowOf(ScreensaverSettings())).distinctUntilChanged()
