package com.wh01s17.launcher.feature.workspaces.model

import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.core.tiling.TilingLayout
import com.wh01s17.launcher.core.tiling.toggleFloating
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DeskCodecTest {
    private fun app(
        name: String,
        user: Long = 0,
    ) = TileContent.App(AppKey("com.$name", "com.$name.Main", user))

    private fun roundTrip(desk: Desk) = DeskCodec.decode(Json.parseToJsonElement(DeskCodec.encode(desk).toString()))

    private fun failure(json: String) = (DeskCodec.decode(Json.parseToJsonElement(json)) as DeskCodec.DecodeResult.Failure).message

    @Test
    fun `a desk with tiles, layouts, floating and work apps reads back the same`() {
        val desk =
            Desk()
                .add(app("termux"))
                .add(app("notes"))
                .add(app("teams", user = 10), workspace = 3)
                .update(3) { it.withLayout(TilingLayout.Master) }
                .updateCurrent { it.toggleFloating(TileId(2)) }
                .focusWorkspace(3)
        assertEquals(DeskCodec.DecodeResult.Success(desk), roundTrip(desk))
    }

    @Test
    fun `widget and native tiles keep their id`() {
        val desk = Desk().add(app("termux")).add(TileContent.Widget(42)).add(TileContent.Native("pomodoro"))
        assertEquals(DeskCodec.DecodeResult.Success(desk), roundTrip(desk))
        assertTrue(DeskCodec.encode(desk).toString().contains("\"widget\":42"))
    }

    @Test
    fun `random desks round trip`() {
        repeat(SEEDS) { seed ->
            val random = Random(seed)
            var desk = Desk()
            repeat(STEPS) {
                desk =
                    when (random.nextInt(4)) {
                        0, 1 -> desk.add(app("a$it"), workspace = random.nextInt(1, 11))
                        2 ->
                            desk.contents.keys
                                .randomOrNull(random)
                                ?.let(desk::close) ?: desk
                        else ->
                            desk.contents.keys
                                .randomOrNull(random)
                                ?.let { desk.move(it, random.nextInt(1, 11), random.nextBoolean()) }
                                ?: desk
                    }
                assertEquals("seed $seed step $it", DeskCodec.DecodeResult.Success(desk), roundTrip(desk))
            }
        }
    }

    @Test
    fun `broken files fail with a reason instead of giving a broken desk`() {
        val emptyWorkspaces = List(Desk.COUNT) { "{}" }.joinToString(",", "[", "]")
        assertTrue(failure("[]").contains("object"))
        assertTrue(failure("""{"current":1}""").contains("version"))
        assertTrue(failure("""{"version":2}""").contains("newer"))
        assertTrue(failure("""{"version":1,"workspaces":{}}""").contains("workspaces"))
        assertTrue(failure("""{"version":1,"workspaces":[{"layout":"spiral"}]}""").contains("workspace 1"))
        assertTrue(failure("""{"version":1,"workspaces":$emptyWorkspaces}""").contains("tiles"))
        assertTrue(failure("""{"version":1,"workspaces":$emptyWorkspaces,"tiles":[]}""").contains("nextId"))
        assertTrue(failure("""{"version":1,"workspaces":[],"tiles":[],"nextId":1}""").contains("10 workspaces"))
        assertTrue(failure("""{"version":1,"workspaces":$emptyWorkspaces,"tiles":[{"id":1}],"nextId":2}""").contains("content"))
        assertTrue(failure("""{"version":1,"workspaces":$emptyWorkspaces,"tiles":[1],"nextId":2}""").contains("objects"))
        assertTrue(failure("""{"version":1,"workspaces":$emptyWorkspaces,"tiles":[{"app":{}}],"nextId":2}""").contains("no id"))
        assertTrue(failure("""{"version":1,"workspaces":$emptyWorkspaces,"tiles":[{"id":1,"app":{}}],"nextId":2}""").contains("package"))
        assertTrue(
            failure(
                """{"version":1,"workspaces":$emptyWorkspaces,"tiles":[{"id":1,"app":{"package":"a"}}],"nextId":2}""",
            ).contains("class"),
        )
        assertTrue(
            failure("""{"version":1,"workspaces":$emptyWorkspaces,"tiles":[{"id":1,"app":{"package":"a","class":"b"}}],"nextId":2}""")
                .contains("user"),
        )
        // Un tile sin sitio en ningún workspace rompe las invariantes del Desk.
        assertTrue(
            failure(
                """{"version":1,"workspaces":$emptyWorkspaces,"tiles":[{"id":1,"app":{"package":"a","class":"b","user":0}}],"nextId":2}""",
            ).contains("do not match"),
        )
    }

    private companion object {
        const val SEEDS = 40
        const val STEPS = 40
    }
}
