package com.wh01s17.launcher.core.config

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * `launcher.json` (plan.md §6). Por ahora sólo se lee `bar`; el resto del archivo se ignora (las
 * secciones de workspaces, atajos y animaciones se añaden en sus hitos, con su migración si hace
 * falta).
 */
data class LauncherConfig(
    val version: Int,
    val bar: BarConfig,
)

/** `bar` de `launcher.json`, igual que `bar` de `shell.json` del escritorio. */
data class BarConfig(
    val position: BarPosition = BarPosition.Top,
    /** Sin fondo propio: el fondo del launcher se ve detrás de la barra. */
    val transparent: Boolean = true,
    /** Contorno con el borde activo de las ventanas (verde → cian). */
    val outline: Boolean = false,
    /** Id del módulo que se mantiene centrado en la pantalla. */
    val centerAnchor: String? = null,
    val layout: BarLayoutConfig = BarLayoutConfig(),
)

enum class BarPosition(
    val configName: String,
) {
    Top("top"),
}

/** Los tres grupos de la barra, de izquierda a derecha. */
data class BarLayoutConfig(
    val left: List<BarModuleConfig> = emptyList(),
    val center: List<BarModuleConfig> = emptyList(),
    val right: List<BarModuleConfig> = emptyList(),
) {
    val all: List<BarModuleConfig> get() = left + center + right
}

/**
 * Un módulo de la barra: su `id` y el resto de claves de la entrada, que cada módulo interpreta
 * (`format`, `classColors`, `brackets`…). Los ayudantes devuelven `null` si la clave falta o no
 * tiene el tipo pedido; el parser ya validó las opciones comunes.
 */
data class BarModuleConfig(
    val id: String,
    val options: Map<String, JsonElement> = emptyMap(),
) {
    fun string(key: String): String? = (options[key] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

    fun bool(key: String): Boolean? = (options[key] as? JsonPrimitive)?.booleanOrNull

    fun int(key: String): Int? = (options[key] as? JsonPrimitive)?.intOrNull

    fun stringList(key: String): List<String>? =
        (options[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.contentOrNull }

    fun stringMap(key: String): Map<String, String>? =
        (options[key] as? JsonObject)
            ?.mapNotNull { (name, value) -> (value as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull?.let { name to it } }
            ?.toMap()
}
