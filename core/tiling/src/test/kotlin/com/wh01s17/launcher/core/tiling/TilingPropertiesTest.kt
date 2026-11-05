package com.wh01s17.launcher.core.tiling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Pruebas de propiedades (plan.md §5.4 y §9): secuencias aleatorias de operaciones, con semilla
 * fija para que un fallo se pueda reproducir, comprobando las invariantes en cada paso.
 */
class TilingPropertiesTest {
    private val areas = listOf(Rect(0, 0, 1220, 2600), Rect(0, 0, 2600, 1220), Rect(10, 20, 400, 300), Rect(0, 0, 37, 23))
    private val gapsChoices = listOf(Gaps(0, 0), Gaps(4, 2), Gaps(10, 6), Gaps(30, 0))

    private class Run(
        seed: Int,
    ) {
        val random = Random(seed)
        var nextId = 1L

        fun newId() = TileId(nextId++)
    }

    /** Una operación cualquiera sobre [ws], elegida al azar: una rama por operación del motor. */
    @Suppress("CyclomaticComplexMethod")
    private fun Run.step(
        ws: Workspace,
        area: Rect,
        gaps: Gaps,
    ): Workspace {
        val existing = ws.tiles
        val direction = Direction.entries.random(random)
        return when (random.nextInt(15)) {
            0, 1, 2 -> ws.insert(newId())
            3 -> if (existing.isEmpty()) ws else ws.remove(existing.random(random))
            4 -> if (existing.isEmpty()) ws else ws.focus(existing.random(random))
            5 -> ws.moveFocus(direction, area, gaps)
            6 -> ws.swapInDirection(direction, area, gaps)
            7 -> ws.resize(direction, random.nextInt(-300, 300), area, gaps)
            8 -> ws.toggleSplit(area, gaps)
            9 -> ws.toggleFloating()
            10 -> if (random.nextInt(4) == 0) ws.toggleFullscreen() else ws.cycleGroup(random.nextBoolean())
            11 -> ws.moveIntoGroup(direction, area, gaps)
            12 -> if (random.nextBoolean()) ws.toggleGroup() else ws.moveOutOfGroup()
            13 -> if (existing.size < 2) ws else ws.groupWith(existing.random(random), existing.random(random))
            else -> ws.withLayout(TilingLayout.entries.random(random))
        }
    }

    private fun checkInvariants(
        ws: Workspace,
        area: Rect,
        gaps: Gaps,
    ) {
        val result = layout(ws, area, gaps)
        val outer = area.inset(gaps.outer)
        val tiled = result.tiled.values.toList()
        tiled.forEach { assertTrue(outer.contains(it), "$it outside $outer") }
        result.floating.values.forEach { assertTrue(outer.contains(it), "floating $it outside $outer") }
        for (i in tiled.indices) {
            for (j in i + 1 until tiled.size) assertTrue(!tiled[i].overlaps(tiled[j]), "${tiled[i]} overlaps ${tiled[j]} in $ws")
        }
        if (ws.fullscreen != null) {
            assertEquals(setOf(ws.fullscreen), result.visible.keys)
            return
        }
        // Se ve exactamente la pestaña activa de cada hueco más los flotantes.
        assertEquals(
            ws.root
                ?.leaves()
                ?.map { it.shown }
                ?.toSet()
                .orEmpty(),
            result.tiled.keys,
        )
        assertEquals(ws.floating.map { it.id }.toSet(), result.floating.keys)
        // Sin huecos, el mosaico cubre el área exacta: ni un píxel de más ni de menos.
        if (gaps.inner == 0 && ws.root != null) assertEquals(outer.area, tiled.sumOf { it.area })
    }

    @Test
    fun `random operations keep every invariant`() {
        repeat(SEQUENCES) { seed ->
            val run = Run(seed)
            val area = areas[seed % areas.size]
            val gaps = gapsChoices[(seed / areas.size) % gapsChoices.size]
            var ws = Workspace(maxTiles = 1 + seed % Workspace.DEFAULT_MAX_TILES)
            repeat(STEPS) {
                ws = run.step(ws, area, gaps)
                checkInvariants(ws, area, gaps)
            }
        }
    }

    @Test
    fun `remove of insert is the identity`() {
        repeat(SEQUENCES) { seed ->
            val run = Run(seed)
            val area = areas[seed % areas.size]
            var ws = Workspace(maxTiles = 1 + seed % Workspace.DEFAULT_MAX_TILES)
            repeat(STEPS) {
                ws = run.step(ws, area, Gaps(4, 2))
                val probe = TileId(1_000_000L + it)
                // Pantalla completa se apaga al insertar: la propiedad vale cuando no la hay.
                if (ws.fullscreen == null) assertEquals(ws, ws.insert(probe).remove(probe), "seed $seed step $it")
            }
        }
    }

    @Test
    fun `serialization round trips every state`() {
        repeat(SEQUENCES) { seed ->
            val run = Run(seed)
            var ws = Workspace()
            repeat(STEPS) {
                ws = run.step(ws, areas[0], Gaps(4, 2))
                val decoded = WorkspaceCodec.decode(WorkspaceCodec.encode(ws))
                assertEquals(WorkspaceCodec.DecodeResult.Success(ws), decoded, "seed $seed step $it")
            }
        }
    }

    @Test
    fun `layout is deterministic`() {
        repeat(SEQUENCES / 4) { seed ->
            val run = Run(seed)
            var ws = Workspace()
            repeat(STEPS) { ws = run.step(ws, areas[0], Gaps(4, 2)) }
            assertEquals(layout(ws, areas[0], Gaps(4, 2)), layout(ws.copy(), areas[0], Gaps(4, 2)))
        }
    }

    private companion object {
        const val SEQUENCES = 200
        const val STEPS = 60
    }
}
