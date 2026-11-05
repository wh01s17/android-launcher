package com.wh01s17.launcher.core.config

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Preferencias de presentación; el tema incluido sigue siendo la fuente de todos los tokens. */
data class AppearanceConfig(
    val theme: String = "wh01s17",
    val fontScale: Float = DEFAULT_FONT_SCALE,
    val wallpaper: String = "none",
    /** Paleta de tidalamp que tiñe todo el launcher; una desconocida se pinta con `phosphor`. */
    val palette: String = DEFAULT_PALETTE,
) {
    companion object {
        const val DEFAULT_FONT_SCALE = 1.25f
        const val DEFAULT_PALETTE = "phosphor"
        val WALLPAPERS = setOf("none", "mark", "moon", "moon-wordmark", "imported")
    }
}

data class IdleConfig(
    val screensaver: Int = DEFAULT_SECONDS,
) {
    companion object {
        const val DEFAULT_SECONDS = 150
        val SECONDS = 0..3600
    }
}

internal object AppearanceSections {
    fun read(
        root: JsonObject,
        defaults: AppearanceConfig,
        errors: MutableList<ConfigError>,
    ): AppearanceConfig {
        val theme = ConfigValues.readString(root["theme"], "theme", errors) ?: defaults.theme
        if (theme != "wh01s17") errors += ConfigError("theme", "tema no instalado: $theme")
        val appearance = section(root["appearance"], "appearance", errors)
        val scale =
            ConfigValues.readNumber(appearance["fontScale"], "appearance.fontScale", MIN_SCALE, MAX_SCALE, errors)
                ?: defaults.fontScale
        val wallpaper = ConfigValues.readString(appearance["wallpaper"], "appearance.wallpaper", errors) ?: defaults.wallpaper
        if (wallpaper !in AppearanceConfig.WALLPAPERS) errors += ConfigError("appearance.wallpaper", "fondo desconocido: $wallpaper")
        val palette = ConfigValues.readString(appearance["palette"], "appearance.palette", errors) ?: defaults.palette
        if (!PALETTE_NAME.matches(palette)) errors += ConfigError("appearance.palette", "nombre de paleta inválido: $palette")
        return AppearanceConfig(theme, scale, wallpaper, palette)
    }

    fun idle(
        element: JsonElement?,
        defaults: IdleConfig,
        errors: MutableList<ConfigError>,
    ): IdleConfig {
        val idle = section(element, "idle", errors)
        return IdleConfig(ConfigValues.readInt(idle["screensaver"], "idle.screensaver", IdleConfig.SECONDS, errors) ?: defaults.screensaver)
    }

    private fun section(
        element: JsonElement?,
        path: String,
        errors: MutableList<ConfigError>,
    ): JsonObject {
        if (element != null && element !is JsonObject) errors += ConfigError(path, "debe ser un objeto")
        return element as? JsonObject ?: JsonObject(emptyMap())
    }

    private const val MIN_SCALE = 0.74f
    private const val MAX_SCALE = 2f
    private val PALETTE_NAME = Regex("[a-z0-9-]+")
}
