package com.wh01s17.launcher.core.config

import com.wh01s17.launcher.core.tiling.TilingLayout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * `general` y `animations` de `launcher.json`. Igual que `bar`, lo que falta toma el valor de
 * [GeneralConfig]/[AnimationsConfig] por defecto (los de la config de fábrica al leer la del usuario).
 */
internal object GeneralSections {
    fun readGeneral(
        element: JsonElement?,
        defaults: GeneralConfig,
        errors: MutableList<ConfigError>,
    ): GeneralConfig {
        val general = section(element, "general", errors) ?: return defaults

        fun int(
            key: String,
            range: IntRange,
        ) = ConfigValues.readInt(general[key], "general.$key", range, errors)
        return GeneralConfig(
            layout = readLayout(general["layout"], errors) ?: defaults.layout,
            maxTilesPerWorkspace = int("maxTilesPerWorkspace", GeneralConfig.MAX_TILES) ?: defaults.maxTilesPerWorkspace,
            gapsIn = int("gapsIn", GeneralConfig.GAPS) ?: defaults.gapsIn,
            gapsOut = int("gapsOut", GeneralConfig.GAPS) ?: defaults.gapsOut,
            borderSize = int("borderSize", GeneralConfig.BORDER) ?: defaults.borderSize,
            rounding = int("rounding", GeneralConfig.ROUNDING) ?: defaults.rounding,
        )
    }

    fun readAnimations(
        element: JsonElement?,
        defaults: AnimationsConfig,
        errors: MutableList<ConfigError>,
    ): AnimationsConfig {
        val animations = section(element, "animations", errors) ?: return defaults
        val workspaces = section(animations["workspaces"], "animations.workspaces", errors) ?: return defaults
        val path = "animations.workspaces"
        val base = defaults.workspaces
        ConfigValues.readString(workspaces["style"], "$path.style", errors)?.let { style ->
            if (style !=
                WorkspaceAnimation.STYLE
            ) {
                errors +=
                    ConfigError("$path.style", "«$style» no está disponible; sólo «${WorkspaceAnimation.STYLE}»")
            }
        }
        return AnimationsConfig(
            WorkspaceAnimation(
                enabled = ConfigValues.readBoolean(workspaces["enabled"], "$path.enabled", errors) ?: base.enabled,
                curve = readCurve(workspaces["bezier"], "$path.bezier", errors) ?: base.curve,
                speed = ConfigValues.readNumber(workspaces["speed"], "$path.speed", 0f, WorkspaceAnimation.MAX_SPEED, errors) ?: base.speed,
            ),
        )
    }

    /** `apps.terminal`: texto no vacío; si es una actividad, `paquete/clase`. Lo que falta conserva [defaults]. */
    fun readApps(
        element: JsonElement?,
        defaults: AppsConfig,
        errors: MutableList<ConfigError>,
    ): AppsConfig {
        val apps = section(element, "apps", errors) ?: return defaults
        val terminal = ConfigValues.readString(apps["terminal"], "apps.terminal", errors) ?: return defaults
        val slash = terminal.indexOf('/')
        val valid = terminal.isNotBlank() && ' ' !in terminal && (slash == -1 || slash in 1 until terminal.lastIndex)
        if (!valid) {
            errors +=
                ConfigError("apps.terminal", "debe ser un paquete (com.termux) o una actividad (com.termux/.app.TermuxActivity)")
        }
        return AppsConfig(terminal.takeIf { valid } ?: defaults.terminal)
    }

    /** `notifications.toast` y `notifications.toastSeconds`; lo que falta conserva [defaults]. */
    fun readNotifications(
        element: JsonElement?,
        defaults: NotificationsConfig,
        errors: MutableList<ConfigError>,
    ): NotificationsConfig {
        val notifications = section(element, "notifications", errors) ?: return defaults
        return NotificationsConfig(
            toast = ConfigValues.readBoolean(notifications["toast"], "notifications.toast", errors) ?: defaults.toast,
            toastSeconds =
                ConfigValues.readInt(notifications["toastSeconds"], "notifications.toastSeconds", NotificationsConfig.TOAST_SECONDS, errors)
                    ?: defaults.toastSeconds,
        )
    }

    /** El objeto de [path], o `null` si falta (sin error) o no es un objeto (con error). */
    private fun section(
        element: JsonElement?,
        path: String,
        errors: MutableList<ConfigError>,
    ): JsonObject? {
        if (element != null && element !is JsonObject) errors += ConfigError(path, "debe ser un objeto")
        return element as? JsonObject
    }

    private fun readLayout(
        element: JsonElement?,
        errors: MutableList<ConfigError>,
    ): TilingLayout? {
        val name = ConfigValues.readString(element, "general.layout", errors) ?: return null
        return TilingLayout.fromConfig(name).also {
            if (it == null) {
                errors +=
                    ConfigError(
                        "general.layout",
                        "«$name» no es válido; opciones: ${TilingLayout.entries.joinToString { l ->
                            l.configName
                        }}",
                    )
            }
        }
    }

    private fun readCurve(
        element: JsonElement?,
        path: String,
        errors: MutableList<ConfigError>,
    ): AnimationCurve? {
        val name = ConfigValues.readString(element, path, errors) ?: return null
        return AnimationCurve.fromConfig(name).also {
            if (it ==
                null
            ) {
                errors +=
                    ConfigError(path, "«$name» no es válida; opciones: ${AnimationCurve.entries.joinToString { c -> c.configName }}")
            }
        }
    }
}
