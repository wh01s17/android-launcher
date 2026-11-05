package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.ui.geometry.Offset
import com.wh01s17.launcher.core.tiling.Direction
import com.wh01s17.launcher.core.tiling.Rect
import com.wh01s17.launcher.core.tiling.TileId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DropZoneTest {
    private val tile = Rect(0, 0, 400, 800)

    @Test
    fun `the center groups and each edge band picks its side`() {
        assertEquals(DropZone.Group, dropZone(tile, Offset(200f, 400f)))
        assertEquals(DropZone.Side(Direction.Down), dropZone(tile, Offset(200f, 760f)))
        assertEquals(DropZone.Side(Direction.Up), dropZone(tile, Offset(200f, 30f)))
        assertEquals(DropZone.Side(Direction.Left), dropZone(tile, Offset(20f, 400f)))
        assertEquals(DropZone.Side(Direction.Right), dropZone(tile, Offset(390f, 300f)))
    }

    @Test
    fun `edges count in proportion to the tile so a tall tile still has left and right`() {
        // A 50 px del borde izquierdo (12 %) y a 150 px del de arriba (19 %): gana la izquierda.
        assertEquals(DropZone.Side(Direction.Left), dropZone(tile, Offset(50f, 150f)))
    }

    @Test
    fun `the highlight is the half that the tile will take`() {
        val chess = TileId(1)
        val target = dropTargetAt(Offset(200f, 760f), TileId(2), mapOf(chess to tile))!!
        assertEquals(chess, target.tile)
        assertEquals(Rect(0, 400, 400, 800), target.highlight)
        assertEquals(tile, dropTargetAt(Offset(200f, 400f), TileId(2), mapOf(chess to tile))!!.highlight)
        // Sobre sí mismo o fuera de todo tile no hay destino.
        assertNull(dropTargetAt(Offset(200f, 400f), chess, mapOf(chess to tile)))
        assertNull(dropTargetAt(Offset(900f, 400f), TileId(2), mapOf(chess to tile)))
    }
}
