package com.wh01s17.launcher.core.config

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Lectura y validación de valores sueltos de `launcher.json`, anotando los problemas con su ruta. */
internal object ConfigValues {
    private val COLOR_MAP_OPTIONS = setOf("classColors")
    private val COLOR_OPTIONS = setOf("bracketColor")
    private val STRING_LIST_OPTIONS = setOf("dimClasses")
    private val BOOLEAN_OPTIONS = setOf("brackets")

    fun readBoolean(
        element: JsonElement?,
        path: String,
        errors: MutableList<ConfigError>,
    ): Boolean? {
        if (element == null) return null
        val value = (element as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull
        if (value == null) errors += ConfigError(path, "debe ser true o false")
        return value
    }

    fun readString(
        element: JsonElement?,
        path: String,
        errors: MutableList<ConfigError>,
    ): String? {
        if (element == null) return null
        val text = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (text == null) errors += ConfigError(path, "debe ser un texto")
        return text
    }

    /** Las opciones comunes a todos los módulos (`classColors`, `dimClasses`, `brackets`) tienen tipo fijo. */
    fun validateOptions(
        entry: JsonObject,
        path: String,
        errors: MutableList<ConfigError>,
    ) {
        for ((key, value) in entry) {
            val where = "$path.$key"
            when (key) {
                in COLOR_MAP_OPTIONS -> validateColorMap(value, where, errors)
                in COLOR_OPTIONS -> validateColor(value, where, errors)
                in STRING_LIST_OPTIONS -> validateStringList(value, where, errors)
                in BOOLEAN_OPTIONS -> readBoolean(value, where, errors)
            }
        }
    }

    private fun validateColorMap(
        value: JsonElement,
        path: String,
        errors: MutableList<ConfigError>,
    ) {
        if (value is JsonObject) {
            for ((name, color) in value) validateColor(color, "$path.$name", errors)
        } else {
            errors += ConfigError(path, "debe ser un objeto «clase»: «#color»")
        }
    }

    private fun validateColor(
        value: JsonElement,
        path: String,
        errors: MutableList<ConfigError>,
    ) {
        val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (text == null || HexColor.parse(text) == null) errors += ConfigError(path, "debe ser un color #RRGGBB (o #RGB, #RRGGBBAA)")
    }

    private fun validateStringList(
        value: JsonElement,
        path: String,
        errors: MutableList<ConfigError>,
    ) {
        val valid = value is JsonArray && value.all { it is JsonPrimitive && it.isString }
        if (!valid) errors += ConfigError(path, "debe ser una lista de textos")
    }
}
