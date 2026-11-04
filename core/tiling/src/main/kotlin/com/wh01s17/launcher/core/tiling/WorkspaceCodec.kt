package com.wh01s17.launcher.core.tiling

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Un [Workspace] como JSON, para guardarlo (plan.md §5.4 `serialize/deserialize`, §6). Formato:
 *
 * ```json
 * { "layout": "dwindle", "focused": 2, "fullscreen": null, "masterRatio": 0.55, "maxTiles": 8,
 *   "root": { "split": "horizontal", "ratio": 0.5, "first": { "tiles": [1] }, "second": { "tiles": [2, 3], "active": 1 } },
 *   "floating": [ { "id": 4, "x": 0.2, "y": 0.2, "width": 0.6, "height": 0.6 } ] }
 * ```
 *
 * `split` vale `horizontal`, `vertical` o `null` (automático). Leer comprueba las mismas reglas que
 * el constructor: un archivo roto o editado a mano da [DecodeResult.Failure], nunca un estado
 * inválido.
 */
object WorkspaceCodec {
    fun encode(workspace: Workspace): JsonObject =
        buildJsonObject {
            put("layout", workspace.layout.configName)
            put("focused", workspace.focused?.value)
            put("fullscreen", workspace.fullscreen?.value)
            put("masterRatio", workspace.masterRatio)
            put("maxTiles", workspace.maxTiles)
            put("root", workspace.root?.let(::encodeNode) ?: JsonNull)
            put("floating", JsonArray(workspace.floating.map(::encodeFloating)))
            put("history", JsonArray(workspace.history.map { JsonPrimitive(it.value) }))
        }

    sealed interface DecodeResult {
        data class Success(
            val workspace: Workspace,
        ) : DecodeResult

        data class Failure(
            val message: String,
        ) : DecodeResult
    }

    fun decode(element: JsonElement): DecodeResult =
        try {
            DecodeResult.Success(read(element as? JsonObject ?: fail("workspace must be an object")))
        } catch (e: IllegalArgumentException) {
            DecodeResult.Failure(e.message ?: "invalid workspace")
        } catch (e: IllegalStateException) {
            DecodeResult.Failure(e.message ?: "invalid workspace")
        }

    private fun read(json: JsonObject): Workspace =
        Workspace(
            root = json["root"]?.takeIf { it !is JsonNull }?.let(::decodeNode),
            layout = json.string("layout")?.let { TilingLayout.fromConfig(it) ?: fail("unknown layout «$it»") } ?: TilingLayout.Dwindle,
            focused = json.long("focused")?.let(::TileId),
            floating =
                json["floating"]
                    ?.jsonArray
                    ?.map {
                        decodeFloating(
                            it as? JsonObject ?: fail("floating entries must be objects"),
                        )
                    }.orEmpty(),
            fullscreen = json.long("fullscreen")?.let(::TileId),
            masterRatio = json.float("masterRatio") ?: Workspace.DEFAULT_MASTER_RATIO,
            maxTiles = json.int("maxTiles") ?: Workspace.DEFAULT_MAX_TILES,
            history =
                json["history"]
                    ?.jsonArray
                    ?.map {
                        (it as? JsonPrimitive)?.longOrNull?.let(::TileId)
                            ?: fail("history ids must be numbers")
                    }.orEmpty(),
        )

    private fun encodeNode(node: Node): JsonObject =
        when (node) {
            is Leaf ->
                buildJsonObject {
                    put("tiles", JsonArray(node.tiles.map { JsonPrimitive(it.value) }))
                    if (node.active != 0) put("active", node.active)
                }
            is Split ->
                buildJsonObject {
                    put("split", node.orientation?.name?.lowercase())
                    put("ratio", node.ratio)
                    put("first", encodeNode(node.first))
                    put("second", encodeNode(node.second))
                }
        }

    private fun decodeNode(element: JsonElement): Node {
        val json = element as? JsonObject ?: fail("nodes must be objects")
        return if (json.containsKey("tiles")) {
            val tiles =
                json["tiles"]?.jsonArray?.map {
                    (it as? JsonPrimitive)?.longOrNull?.let(::TileId)
                        ?: fail("tile ids must be numbers")
                }
            Leaf(tiles ?: fail("a leaf needs tiles"), json.int("active") ?: 0)
        } else {
            val orientation =
                json.string("split")?.let { name ->
                    Orientation.entries.firstOrNull { it.name.lowercase() == name }
                        ?: fail("unknown split «$name»")
                }
            Split(
                first = decodeNode(json["first"] ?: fail("a split needs «first»")),
                second = decodeNode(json["second"] ?: fail("a split needs «second»")),
                ratio = json.float("ratio") ?: SplitRatio.HALF,
                orientation = orientation,
            )
        }
    }

    private fun encodeFloating(tile: FloatingTile): JsonObject =
        buildJsonObject {
            put("id", tile.id.value)
            put("x", tile.bounds.x)
            put("y", tile.bounds.y)
            put("width", tile.bounds.width)
            put("height", tile.bounds.height)
        }

    private fun decodeFloating(json: JsonObject): FloatingTile =
        FloatingTile(
            id = TileId(json.long("id") ?: fail("a floating tile needs an id")),
            bounds =
                FloatingBounds(
                    x = json.float("x") ?: FloatingBounds.Default.x,
                    y = json.float("y") ?: FloatingBounds.Default.y,
                    width = json.float("width") ?: FloatingBounds.Default.width,
                    height = json.float("height") ?: FloatingBounds.Default.height,
                ),
        )

    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
}

private fun JsonObject.primitive(key: String): JsonPrimitive? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }

private fun JsonObject.string(key: String): String? = primitive(key)?.takeIf { it.isString }?.content

private fun JsonObject.long(key: String): Long? = primitive(key)?.longOrNull

private fun JsonObject.int(key: String): Int? = primitive(key)?.intOrNull

private fun JsonObject.float(key: String): Float? = primitive(key)?.floatOrNull
