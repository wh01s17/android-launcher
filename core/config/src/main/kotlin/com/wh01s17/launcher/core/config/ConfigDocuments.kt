package com.wh01s17.launcher.core.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Combina los campos de los editores sin perder claves desconocidas ni atajos del usuario. */
internal object ConfigDocuments {
    private val json = Json { prettyPrint = true }
    const val MAX_BYTES = 1_048_576

    fun merge(
        text: String?,
        patch: JsonObject,
    ): String {
        val root =
            text?.let { Json.parseToJsonElement(it) as JsonObject }
                ?: JsonObject(mapOf("version" to JsonPrimitive(ConfigParser.SUPPORTED_VERSION)))
        return json.encodeToString(JsonElement.serializer(), mergeObjects(root, patch)) + "\n"
    }

    private fun mergeObjects(
        root: JsonObject,
        patch: JsonObject,
    ): JsonObject =
        JsonObject(
            root.toMutableMap().apply {
                patch.forEach { (key, value) ->
                    val old = this[key]
                    this[key] = if (old is JsonObject && value is JsonObject) mergeObjects(old, value) else value
                }
            },
        )
}
