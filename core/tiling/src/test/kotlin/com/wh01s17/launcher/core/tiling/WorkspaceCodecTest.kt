package com.wh01s17.launcher.core.tiling

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkspaceCodecTest {
    private fun decode(json: String) = WorkspaceCodec.decode(Json.parseToJsonElement(json))

    private fun failure(json: String) = (decode(json) as WorkspaceCodec.DecodeResult.Failure).message

    @Test
    fun `the documented format reads back`() {
        val ws =
            decode(
                """{"layout":"dwindle","focused":2,"fullscreen":null,"masterRatio":0.55,"maxTiles":8,
                "root":{"split":"horizontal","ratio":0.5,"first":{"tiles":[1]},"second":{"tiles":[2,3],"active":1}},
                "floating":[{"id":4,"x":0.2,"y":0.2,"width":0.6,"height":0.6}]}""",
            )
        val workspace = (ws as WorkspaceCodec.DecodeResult.Success).workspace
        assertEquals(listOf(TileId(1), TileId(2), TileId(3), TileId(4)), workspace.tiles)
        assertEquals(Orientation.Horizontal, (workspace.root as Split).orientation)
        assertEquals(TileId(2), workspace.focused)
    }

    @Test
    fun `missing optional fields take their defaults`() {
        val workspace = (decode("""{"root":{"tiles":[7]}}""") as WorkspaceCodec.DecodeResult.Success).workspace
        assertEquals(Workspace(root = Leaf.of(TileId(7))), workspace)
        assertEquals(WorkspaceCodec.DecodeResult.Success(Workspace()), decode("{}"))
    }

    @Test
    fun `broken files give a readable failure, never an invalid state`() {
        assertTrue(failure("[]").contains("object"))
        assertTrue(failure("""{"layout":"spiral"}""").contains("spiral"))
        assertTrue(failure("""{"root":{"tiles":[1]},"focused":5}""").contains("focused"))
        assertTrue(failure("""{"root":{"split":"diagonal","first":{"tiles":[1]},"second":{"tiles":[2]}}}""").contains("diagonal"))
        assertTrue(failure("""{"root":{"first":{"tiles":[1]}}}""").contains("second"))
        assertTrue(failure("""{"root":{"split":null,"ratio":0.99,"first":{"tiles":[1]},"second":{"tiles":[2]}}}""").contains("ratio"))
        assertTrue(failure("""{"root":{"tiles":["x"]}}""").contains("numbers"))
        assertTrue(failure("""{"root":{"tiles":[1,1]}}""").contains("duplicate"))
        assertTrue(failure("""{"root":{"tiles":[1]},"floating":[{"id":1}]}""").contains("twice"))
        assertTrue(failure("""{"floating":[{"x":0.1}]}""").contains("id"))
        assertTrue(failure("""{"floating":[1]}""").contains("objects"))
        assertTrue(failure("""{"root":5}""").contains("objects"))
        assertTrue(failure("""{"root":{"tiles":[1],"active":3}}""").contains("active"))
    }
}
