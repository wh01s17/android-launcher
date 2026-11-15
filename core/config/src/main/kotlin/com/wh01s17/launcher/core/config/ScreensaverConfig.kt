package com.wh01s17.launcher.core.config

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * `screensaver` de `launcher.json`: qué muestra el salvapantallas (la W, la hora, un texto o sólo
 * datos), qué líneas añade debajo y cómo se ve. Siempre sobre negro puro OLED; el brillo baja el color
 * (menos consumo), y la deriva anti-quemado y los pocos fotogramas por segundo no se configuran.
 * Cuándo arranca en el inicio sigue en `idle.screensaver`.
 */
data class ScreensaverConfig(
    val style: String = DEFAULT_STYLE,
    /** Lo que muestra el estilo `text` y la línea de texto ([showText]). */
    val text: String = DEFAULT_TEXT,
    val showTime: Boolean = false,
    val showDate: Boolean = false,
    val showBattery: Boolean = false,
    val showText: Boolean = false,
    /** El texto del estilo `text` gira en 3D, como la W. */
    val spinText: Boolean = false,
    val color: String = DEFAULT_COLOR,
    /** Porcentaje del color: menos brillo, menos píxeles OLED encendidos a fondo. */
    val brightness: Int = MAX_PERCENT,
    /** Porcentaje de la velocidad del giro de la W. */
    val speed: Int = MAX_PERCENT,
    /** Porcentaje del tamaño de lo principal (W, hora o texto). */
    val size: Int = MAX_PERCENT,
) {
    companion object {
        const val DEFAULT_STYLE = "w"
        const val DEFAULT_TEXT = "wh01s17"
        const val DEFAULT_COLOR = "accent"
        const val MAX_PERCENT = 100
        const val MAX_TEXT = 24
        val STYLES = listOf("w", "clock", "text", "minimal")
        val COLORS = listOf("accent", "cyan", "foreground", "red", "yellow", "blue", "magenta")
        val BRIGHTNESS = 10..100
        val SPEED = 25..200
        val SIZE = 40..100
    }
}

internal object ScreensaverSections {
    fun read(
        element: JsonElement?,
        defaults: ScreensaverConfig,
        errors: MutableList<ConfigError>,
    ): ScreensaverConfig {
        if (element != null && element !is JsonObject) errors += ConfigError("screensaver", "debe ser un objeto")
        val read = SectionReader(element as? JsonObject ?: JsonObject(emptyMap()), errors)
        return ScreensaverConfig(
            style = read.option("style", defaults.style, ScreensaverConfig.STYLES),
            text = read.text("text", defaults.text),
            showTime = read.flag("showTime", defaults.showTime),
            showDate = read.flag("showDate", defaults.showDate),
            showBattery = read.flag("showBattery", defaults.showBattery),
            showText = read.flag("showText", defaults.showText),
            spinText = read.flag("spinText", defaults.spinText),
            color = read.option("color", defaults.color, ScreensaverConfig.COLORS),
            brightness = read.percent("brightness", defaults.brightness, ScreensaverConfig.BRIGHTNESS),
            speed = read.percent("speed", defaults.speed, ScreensaverConfig.SPEED),
            size = read.percent("size", defaults.size, ScreensaverConfig.SIZE),
        )
    }

    /** Lee las claves de `screensaver` con su valor por defecto y anota los errores con su ruta. */
    private class SectionReader(
        private val section: JsonObject,
        private val errors: MutableList<ConfigError>,
    ) {
        fun flag(
            key: String,
            default: Boolean,
        ) = ConfigValues.readBoolean(section[key], path(key), errors) ?: default

        fun percent(
            key: String,
            default: Int,
            range: IntRange,
        ) = ConfigValues.readInt(section[key], path(key), range, errors) ?: default

        /** Una de [options]; si no lo es, se anota el error (y se guarda igual: se pinta con la de fábrica). */
        fun option(
            key: String,
            default: String,
            options: List<String>,
        ): String {
            val value = ConfigValues.readString(section[key], path(key), errors) ?: default
            if (value !in options) errors += ConfigError(path(key), "«$value» no es válido; opciones: ${options.joinToString()}")
            return value
        }

        fun text(
            key: String,
            default: String,
        ): String {
            val value = ConfigValues.readString(section[key], path(key), errors) ?: default
            val max = ScreensaverConfig.MAX_TEXT
            if (value.length > max) errors += ConfigError(path(key), "máximo $max caracteres")
            return value
        }

        private fun path(key: String) = "screensaver.$key"
    }
}
