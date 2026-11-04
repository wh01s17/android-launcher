package com.wh01s17.launcher.core.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** Un problema de `launcher.json`, con la ruta del valor (`bar.layout.left[2].id`) para poder corregirlo. */
data class ConfigError(
    val path: String,
    val message: String,
) {
    override fun toString() = "$path: $message"
}

sealed interface ParseResult {
    data class Success(
        val config: LauncherConfig,
    ) : ParseResult

    data class Failure(
        val errors: List<ConfigError>,
    ) : ParseResult
}

/**
 * Lee `launcher.json` (plan.md §6) con mensajes legibles. Es estricto con lo que entiende (tipos
 * y valores de `version` y `bar`) y tolerante con lo que no (claves desconocidas se ignoran, para
 * que una config de una versión futura siga abriéndose). Una config con errores no se aplica a
 * medias: devuelve todos los errores a la vez.
 */
object ConfigParser {
    /** Versión del esquema que entiende esta build (plan.md §6.2). */
    const val SUPPORTED_VERSION = 1

    private val json = Json

    /**
     * [fallback] da las secciones que faltan (`bar`): así `{"version": 1}` sigue teniendo barra.
     * La config de fábrica se lee sin fallback y por tanto debe traer todo.
     */
    fun parse(
        text: String,
        fallback: LauncherConfig? = null,
    ): ParseResult {
        val root =
            try {
                json.parseToJsonElement(text)
            } catch (e: IllegalArgumentException) {
                // `SerializationException` también es una `IllegalArgumentException`.
                return failure("$", "JSON no válido: ${e.message?.lineSequence()?.firstOrNull()}")
            }
        val errors = mutableListOf<ConfigError>()
        val config =
            if (root is JsonObject) {
                readRoot(root, fallback, errors)
            } else {
                errors += ConfigError("$", "debe ser un objeto JSON")
                null
            }
        return if (errors.isEmpty() && config != null) ParseResult.Success(config) else ParseResult.Failure(errors)
    }

    private fun failure(
        path: String,
        message: String,
    ) = ParseResult.Failure(listOf(ConfigError(path, message)))

    private fun readRoot(
        root: JsonObject,
        fallback: LauncherConfig?,
        errors: MutableList<ConfigError>,
    ): LauncherConfig? {
        val version = readVersion(root["version"], errors)
        val bar =
            when (val element = root["bar"]) {
                null -> fallback?.bar.also { if (it == null) errors += ConfigError("bar", "falta") }
                is JsonObject -> readBar(element, errors)
                else -> null.also { errors += ConfigError("bar", "debe ser un objeto") }
            }
        return if (version != null && bar != null) LauncherConfig(version, bar) else null
    }

    private fun readVersion(
        element: JsonElement?,
        errors: MutableList<ConfigError>,
    ): Int? {
        val version = (element as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
        val message =
            when {
                element == null -> "falta"
                version == null -> "debe ser un número entero"
                version < 1 -> "debe ser 1 o más"
                version > SUPPORTED_VERSION -> "la versión $version es más nueva que la que entiende esta build ($SUPPORTED_VERSION)"
                else -> null
            }
        message?.let { errors += ConfigError("version", it) }
        return version.takeIf { message == null }
    }

    private fun readBar(
        bar: JsonObject,
        errors: MutableList<ConfigError>,
    ): BarConfig {
        val defaults = BarConfig()
        return BarConfig(
            position = readPosition(bar["position"], errors) ?: defaults.position,
            transparent = ConfigValues.readBoolean(bar["transparent"], "bar.transparent", errors) ?: defaults.transparent,
            outline = ConfigValues.readBoolean(bar["outline"], "bar.outline", errors) ?: defaults.outline,
            centerAnchor = ConfigValues.readString(bar["centerAnchor"], "bar.centerAnchor", errors),
            layout = readLayout(bar["layout"], errors) ?: defaults.layout,
        )
    }

    private fun readPosition(
        element: JsonElement?,
        errors: MutableList<ConfigError>,
    ): BarPosition? {
        val name = ConfigValues.readString(element, "bar.position", errors) ?: return null
        val position = BarPosition.entries.firstOrNull { it.configName == name }
        if (position == null) {
            errors += ConfigError("bar.position", "«$name» no es válido; opciones: ${BarPosition.entries.joinToString { it.configName }}")
        }
        return position
    }

    private fun readLayout(
        element: JsonElement?,
        errors: MutableList<ConfigError>,
    ): BarLayoutConfig? =
        when (element) {
            null -> null
            is JsonObject ->
                BarLayoutConfig(
                    left = readGroup(element["left"], "left", errors),
                    center = readGroup(element["center"], "center", errors),
                    right = readGroup(element["right"], "right", errors),
                )
            else -> null.also { errors += ConfigError("bar.layout", "debe ser un objeto") }
        }

    private fun readGroup(
        element: JsonElement?,
        name: String,
        errors: MutableList<ConfigError>,
    ): List<BarModuleConfig> {
        val path = "bar.layout.$name"
        return when (element) {
            null -> emptyList()
            is JsonArray -> element.mapIndexedNotNull { index, entry -> readModule(entry, "$path[$index]", errors) }
            else -> emptyList<BarModuleConfig>().also { errors += ConfigError(path, "debe ser una lista") }
        }
    }

    private fun readModule(
        element: JsonElement,
        path: String,
        errors: MutableList<ConfigError>,
    ): BarModuleConfig? {
        val entry = element as? JsonObject ?: return null.also { errors += ConfigError(path, "debe ser un objeto con «id»") }
        val id = ConfigValues.readString(entry["id"], "$path.id", errors)
        if (entry["id"] == null) errors += ConfigError("$path.id", "falta")
        ConfigValues.validateOptions(entry, path, errors)
        return id?.let { BarModuleConfig(it, entry.filterKeys { key -> key != "id" }) }
    }
}
