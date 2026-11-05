package com.wh01s17.launcher.feature.workspaces.model

import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.core.tiling.Workspace
import com.wh01s17.launcher.core.tiling.WorkspaceCodec
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * El [Desk] en JSON, para `files/state/workspaces.json`. Es estado de la app, no config del
 * usuario: lo escribe el launcher en cada cambio (plan.md §11.1). Formato:
 *
 * ```json
 * {"version":1,"current":2,"nextId":4,
 *  "workspaces":[{…WorkspaceCodec…}, … 10 …], "scratchpad":{…WorkspaceCodec…},
 *  "tiles":[{"id":1,"app":{"package":"com.termux","class":"com.termux.HomeActivity","user":0}}]}
 * ```
 *
 * `scratchpad` es opcional (los guardados antes del Hito 8 no lo tienen) y si se ve no se guarda.
 *
 * Leer nunca da un estado inválido: o un [Desk] que cumple sus invariantes o un error legible.
 */
object DeskCodec {
    const val VERSION = 1

    sealed interface DecodeResult {
        data class Success(
            val desk: Desk,
        ) : DecodeResult

        data class Failure(
            val message: String,
        ) : DecodeResult
    }

    fun encode(desk: Desk): JsonObject =
        buildJsonObject {
            put("version", VERSION)
            put("current", desk.current)
            put("nextId", desk.nextId)
            put("workspaces", JsonArray(desk.workspaces.map(WorkspaceCodec::encode)))
            put("scratchpad", WorkspaceCodec.encode(desk.scratchpad))
            put(
                "tiles",
                JsonArray(
                    desk.contents.entries
                        .sortedBy { it.key }
                        .map { (id, content) -> encodeTile(id, content) },
                ),
            )
        }

    fun decode(element: JsonElement): DecodeResult =
        try {
            DecodeResult.Success(decodeDesk(element as? JsonObject ?: fail("the desk must be an object")))
        } catch (e: IllegalArgumentException) {
            DecodeResult.Failure(e.message ?: "invalid desk")
        } catch (e: IllegalStateException) {
            DecodeResult.Failure(e.message ?: "invalid desk")
        }

    private fun decodeDesk(json: JsonObject): Desk {
        val version = json.int("version") ?: fail("version is missing")
        if (version > VERSION) fail("version $version is newer than $VERSION")
        val workspaces =
            (json["workspaces"] as? JsonArray ?: fail("workspaces must be a list")).mapIndexed { index, element ->
                when (val result = WorkspaceCodec.decode(element)) {
                    is WorkspaceCodec.DecodeResult.Success -> result.workspace
                    is WorkspaceCodec.DecodeResult.Failure -> fail("workspace ${index + 1}: ${result.message}")
                }
            }
        val scratchpad =
            json["scratchpad"]?.let { element ->
                when (val result = WorkspaceCodec.decode(element)) {
                    is WorkspaceCodec.DecodeResult.Success -> result.workspace
                    is WorkspaceCodec.DecodeResult.Failure -> fail("scratchpad: ${result.message}")
                }
            } ?: Workspace()
        val tiles = (json["tiles"] as? JsonArray ?: fail("tiles must be a list")).associate(::decodeTile)
        return Desk(
            workspaces = workspaces,
            scratchpad = scratchpad,
            current = json.int("current") ?: Desk.FIRST,
            contents = tiles,
            nextId = (json["nextId"] as? JsonPrimitive)?.longOrNull ?: fail("nextId is missing"),
        )
    }

    private fun encodeTile(
        id: TileId,
        content: TileContent,
    ): JsonObject =
        buildJsonObject {
            put("id", id.value)
            when (content) {
                is TileContent.App ->
                    put(
                        "app",
                        buildJsonObject {
                            put("package", content.key.packageName)
                            put("class", content.key.className)
                            put("user", content.key.userSerial)
                        },
                    )
                is TileContent.Widget -> put("widget", content.appWidgetId)
                is TileContent.Native -> put("native", content.id)
            }
        }

    private fun decodeTile(element: JsonElement): Pair<TileId, TileContent> {
        val json = element as? JsonObject ?: fail("tiles must be objects")
        val id = (json["id"] as? JsonPrimitive)?.longOrNull ?: fail("a tile has no id")
        val content =
            json.int("widget")?.let(TileContent::Widget)
                ?: json.string("native")?.let(TileContent::Native)
                ?: decodeApp(json["app"] as? JsonObject ?: fail("tile $id has no content"), id)
        return TileId(id) to content
    }

    private fun decodeApp(
        app: JsonObject,
        id: Long,
    ) = TileContent.App(
        AppKey(
            packageName = app.string("package") ?: fail("tile $id has no package"),
            className = app.string("class") ?: fail("tile $id has no class"),
            userSerial = (app["user"] as? JsonPrimitive)?.longOrNull ?: fail("tile $id has no user"),
        ),
    )

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun fail(message: String): Nothing = throw IllegalArgumentException(message)
}
