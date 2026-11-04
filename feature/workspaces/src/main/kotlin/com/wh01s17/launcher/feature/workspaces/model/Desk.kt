package com.wh01s17.launcher.feature.workspaces.model

import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.core.tiling.TilingLayout
import com.wh01s17.launcher.core.tiling.Workspace
import com.wh01s17.launcher.core.tiling.insert
import com.wh01s17.launcher.core.tiling.remove
import com.wh01s17.launcher.core.tiling.toggleFloating
import com.wh01s17.launcher.core.tiling.withFloatingBounds

/**
 * Todos los escritorios: los 10 workspaces del motor de tiling, qué hay en cada tile y en cuál
 * está el usuario. Inmutable; cada operación devuelve uno nuevo.
 *
 * Invariantes (se comprueban al crearlo): hay exactamente [COUNT] workspaces; cada tile está en uno
 * solo y tiene su [contents]; [current] es un workspace válido y [nextId] es mayor que cualquier id.
 */
data class Desk(
    val workspaces: List<Workspace> = List(COUNT) { Workspace() },
    val current: Int = FIRST,
    val contents: Map<TileId, TileContent> = emptyMap(),
    /** Id del próximo tile: nunca se reutiliza uno, para que nada viejo apunte a un tile nuevo. */
    val nextId: Long = 1,
) {
    init {
        require(workspaces.size == COUNT) { "expected $COUNT workspaces, got ${workspaces.size}" }
        require(current in FIRST..LAST) { "current workspace $current out of range" }
        val all = workspaces.flatMap { it.tiles }
        require(all.toSet().size == all.size) { "a tile is in two workspaces" }
        require(all.toSet() == contents.keys) { "tiles ${all.sorted()} do not match contents ${contents.keys.sorted()}" }
        require(all.all { it.value < nextId }) { "nextId $nextId is not above every tile" }
    }

    fun workspace(id: Int): Workspace = workspaces[id - FIRST]

    val currentWorkspace: Workspace get() = workspace(current)

    /** Workspaces con al menos un tile (los que la barra no oculta). */
    val occupied: Set<Int> get() = (FIRST..LAST).filterTo(mutableSetOf()) { !workspace(it).isEmpty }

    fun workspaceOf(tile: TileId): Int? = (FIRST..LAST).firstOrNull { tile in workspace(it) }

    /** `Super + 1…0`. Un número fuera de rango va al extremo más cercano. */
    fun focusWorkspace(id: Int): Desk = copy(current = id.coerceIn(FIRST, LAST))

    /**
     * `Super + Tab` (`e+1` de Hyprland): el siguiente workspace con tiles, dando la vuelta. Cuenta
     * también el actual aunque esté vacío, para poder volver de él.
     */
    fun cycleWorkspace(forward: Boolean): Desk {
        val ring = (occupied + current).sorted()
        val index = ring.indexOf(current)
        val next = ring[(index + if (forward) 1 else ring.size - 1) % ring.size]
        return copy(current = next)
    }

    /** Añade un tile con [content] al workspace [workspace] (el actual por defecto); recibe el foco. */
    fun add(
        content: TileContent,
        workspace: Int = current,
    ): Desk {
        val id = TileId(nextId)
        return copy(workspaces = replaced(workspace) { it.insert(id) }, contents = contents + (id to content), nextId = nextId + 1)
    }

    /** `Super + W`: quita el tile (la app sigue instalada y abierta si lo estaba). */
    fun close(tile: TileId): Desk {
        val from = workspaceOf(tile) ?: return this
        return copy(workspaces = replaced(from) { it.remove(tile) }, contents = contents - tile)
    }

    /**
     * `Super + Shift + 1…0`: lleva [tile] al workspace [to]; si flotaba, sigue flotando donde estaba.
     * Con [follow] el usuario va con él.
     */
    fun move(
        tile: TileId,
        to: Int,
        follow: Boolean,
    ): Desk {
        val from = workspaceOf(tile) ?: return this
        val target = to.coerceIn(FIRST, LAST)
        if (from == target) return if (follow) focusWorkspace(target) else this
        val floating = workspace(from).floating.firstOrNull { it.id == tile }
        val list = workspaces.toMutableList()
        list[from - FIRST] = list[from - FIRST].remove(tile)
        val inserted = list[target - FIRST].insert(tile)
        list[target - FIRST] = if (floating == null) inserted else inserted.toggleFloating(tile).withFloatingBounds(tile, floating.bounds)
        val moved = copy(workspaces = list)
        return if (follow) moved.focusWorkspace(target) else moved
    }

    /** Cambia un workspace sin añadir ni quitar tiles (eso es [add], [close] y [move]). */
    fun update(
        workspace: Int,
        transform: (Workspace) -> Workspace,
    ): Desk = copy(workspaces = replaced(workspace, transform))

    private fun replaced(
        workspace: Int,
        transform: (Workspace) -> Workspace,
    ): List<Workspace> = workspaces.toMutableList().apply { this[workspace - FIRST] = transform(this[workspace - FIRST]) }

    fun updateCurrent(transform: (Workspace) -> Workspace): Desk = update(current, transform)

    /**
     * Aplica `general` de la config: el máximo de huecos a todos y el layout por defecto a los
     * vacíos (los que tienen tiles conservan el suyo).
     */
    fun withDefaults(
        layout: TilingLayout,
        maxTiles: Int,
    ): Desk = copy(workspaces = workspaces.map { ws -> ws.copy(maxTiles = maxTiles, layout = if (ws.isEmpty) layout else ws.layout) })

    companion object {
        const val FIRST = 1
        const val LAST = 10
        const val COUNT = LAST - FIRST + 1
    }
}
