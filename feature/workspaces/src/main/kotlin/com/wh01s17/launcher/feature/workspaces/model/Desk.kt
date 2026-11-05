package com.wh01s17.launcher.feature.workspaces.model

import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.core.tiling.TilingLayout
import com.wh01s17.launcher.core.tiling.Workspace
import com.wh01s17.launcher.core.tiling.insert
import com.wh01s17.launcher.core.tiling.remove
import com.wh01s17.launcher.core.tiling.toggleFloating
import com.wh01s17.launcher.core.tiling.withFloatingBounds

/**
 * Todos los escritorios: los 10 workspaces del motor de tiling, el scratchpad, qué hay en cada tile
 * y en cuál está el usuario. Inmutable; cada operación devuelve uno nuevo.
 *
 * El scratchpad es el `special:scratchpad` de Hyprland (plan.md §2.1, Hito 8): un escritorio
 * aparte ([SCRATCHPAD]) que se superpone al actual. Mientras se ve ([scratchpadVisible]) es el
 * escritorio **activo** ([active]): añadir, quitar, flotar o mover tiles actúan sobre él. Cambiar
 * de escritorio lo oculta (`hide_special_on_workspace_change`).
 *
 * Invariantes (se comprueban al crearlo): hay exactamente [COUNT] workspaces; cada tile está en uno
 * solo (o en el scratchpad) y tiene su [contents]; [current] es un workspace válido y [nextId] es
 * mayor que cualquier id.
 */
data class Desk(
    val workspaces: List<Workspace> = List(COUNT) { Workspace() },
    val current: Int = FIRST,
    val contents: Map<TileId, TileContent> = emptyMap(),
    /** Id del próximo tile: nunca se reutiliza uno, para que nada viejo apunte a un tile nuevo. */
    val nextId: Long = 1,
    val scratchpad: Workspace = Workspace(),
    /** El scratchpad se ve encima del escritorio actual. No se guarda: al volver, oculto. */
    val scratchpadVisible: Boolean = false,
) {
    init {
        require(workspaces.size == COUNT) { "expected $COUNT workspaces, got ${workspaces.size}" }
        require(current in FIRST..LAST) { "current workspace $current out of range" }
        val all = workspaces.flatMap { it.tiles } + scratchpad.tiles
        require(all.toSet().size == all.size) { "a tile is in two workspaces" }
        require(all.toSet() == contents.keys) { "tiles ${all.sorted()} do not match contents ${contents.keys.sorted()}" }
        require(all.all { it.value < nextId }) { "nextId $nextId is not above every tile" }
    }

    /** Un workspace por número, o el scratchpad con [SCRATCHPAD]. */
    fun workspace(id: Int): Workspace = if (id == SCRATCHPAD) scratchpad else workspaces[id - FIRST]

    val currentWorkspace: Workspace get() = workspace(current)

    /** El escritorio sobre el que actúan los tiles: el scratchpad si se ve, si no el actual. */
    val active: Int get() = if (scratchpadVisible) SCRATCHPAD else current

    val activeWorkspace: Workspace get() = workspace(active)

    /** Workspaces con al menos un tile (los que la barra no oculta); no incluye el scratchpad. */
    val occupied: Set<Int> get() = (FIRST..LAST).filterTo(mutableSetOf()) { !workspace(it).isEmpty }

    fun workspaceOf(tile: TileId): Int? = (FIRST..LAST).firstOrNull { tile in workspace(it) } ?: SCRATCHPAD.takeIf { tile in scratchpad }

    /**
     * `Super + 1…0`. Un número fuera de rango va al extremo más cercano; ir a un escritorio oculta
     * el scratchpad. Con [SCRATCHPAD], lo muestra sin cambiar el actual.
     */
    fun focusWorkspace(id: Int): Desk =
        if (id == SCRATCHPAD) copy(scratchpadVisible = true) else copy(current = id.coerceIn(FIRST, LAST), scratchpadVisible = false)

    /** `Super + S`: muestra u oculta el scratchpad. */
    fun toggleScratchpad(): Desk = copy(scratchpadVisible = !scratchpadVisible)

    /**
     * `Super + Tab` (`e+1` de Hyprland): el siguiente workspace con tiles, dando la vuelta. Cuenta
     * también el actual aunque esté vacío, para poder volver de él. Oculta el scratchpad.
     */
    fun cycleWorkspace(forward: Boolean): Desk {
        val ring = (occupied + current).sorted()
        val index = ring.indexOf(current)
        val next = ring[(index + if (forward) 1 else ring.size - 1) % ring.size]
        return copy(current = next, scratchpadVisible = false)
    }

    /** Añade un tile con [content] al workspace [workspace] (el activo por defecto); recibe el foco. */
    fun add(
        content: TileContent,
        workspace: Int = active,
    ): Desk {
        val id = TileId(nextId)
        return replacing(workspace to workspace(workspace).insert(id), contents = contents + (id to content), nextId = nextId + 1)
    }

    /** `Super + W`: quita el tile (la app sigue instalada y abierta si lo estaba). */
    fun close(tile: TileId): Desk {
        val from = workspaceOf(tile) ?: return this
        return replacing(from to workspace(from).remove(tile), contents = contents - tile)
    }

    /**
     * `Super + Shift + 1…0`: lleva [tile] al workspace [to] ([SCRATCHPAD] con `Super + Alt + S`); si
     * flotaba, sigue flotando donde estaba. Con [follow] el usuario va con él.
     */
    fun move(
        tile: TileId,
        to: Int,
        follow: Boolean,
    ): Desk {
        val from = workspaceOf(tile) ?: return this
        val target = if (to == SCRATCHPAD) SCRATCHPAD else to.coerceIn(FIRST, LAST)
        if (from == target) return if (follow) focusWorkspace(target) else this
        val floating = workspace(from).floating.firstOrNull { it.id == tile }
        val inserted = workspace(target).insert(tile)
        val placed = if (floating == null) inserted else inserted.toggleFloating(tile).withFloatingBounds(tile, floating.bounds)
        val moved = replacing(from to workspace(from).remove(tile), target to placed)
        return if (follow) moved.focusWorkspace(target) else moved
    }

    /** Cambia un workspace (o el scratchpad) sin añadir ni quitar tiles (eso es [add], [close] y [move]). */
    fun update(
        workspace: Int,
        transform: (Workspace) -> Workspace,
    ): Desk = replacing(workspace to transform(workspace(workspace)))

    /**
     * Sustituye varios workspaces (y, si hace falta, el contenido) en un solo `copy`: las
     * invariantes se comprueban con todo ya cambiado.
     */
    private fun replacing(
        vararg changes: Pair<Int, Workspace>,
        contents: Map<TileId, TileContent> = this.contents,
        nextId: Long = this.nextId,
    ): Desk {
        val list = workspaces.toMutableList()
        var special = scratchpad
        for ((id, ws) in changes) if (id == SCRATCHPAD) special = ws else list[id - FIRST] = ws
        return copy(workspaces = list, scratchpad = special, contents = contents, nextId = nextId)
    }

    /** Cambia el escritorio activo: el scratchpad si se ve, si no el actual. */
    fun updateCurrent(transform: (Workspace) -> Workspace): Desk = update(active, transform)

    companion object {
        const val FIRST = 1
        const val LAST = 10
        const val COUNT = LAST - FIRST + 1

        /** El número del scratchpad (`special:scratchpad`): no es ninguno de 1…10. */
        const val SCRATCHPAD = 0
    }
}

/**
 * Aplica `general` de la config: el máximo de huecos a todos (también al scratchpad) y el layout por
 * defecto a los vacíos (los que tienen tiles conservan el suyo).
 */
fun Desk.withDefaults(
    layout: TilingLayout,
    maxTiles: Int,
): Desk {
    val apply = { ws: Workspace -> ws.copy(maxTiles = maxTiles, layout = if (ws.isEmpty) layout else ws.layout) }
    return copy(workspaces = workspaces.map(apply), scratchpad = apply(scratchpad))
}
