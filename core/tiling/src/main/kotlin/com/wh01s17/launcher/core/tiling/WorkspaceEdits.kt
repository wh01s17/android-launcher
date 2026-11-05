package com.wh01s17.launcher.core.tiling

/**
 * Añade [tile] (plan.md §5.4). En dwindle parte la hoja con el foco (o la última si el foco está en
 * un flotante), y el nuevo queda a la derecha o abajo (`force_split = 2` de Hyprland). En master
 * pasa a ser el principal y el anterior encabeza la pila (`new_status = "master"`). Nunca agrupa:
 * los grupos son explícitos ([groupWith], [toggleGroup]), como en Hyprland. El nuevo tile recibe el
 * foco (el anterior pasa al historial) y se sale de pantalla completa.
 *
 * `remove(insert(x))` devuelve el workspace original (propiedad probada).
 */
fun Workspace.insert(tile: TileId): Workspace {
    require(tile !in this) { "$tile is already in the workspace" }
    val root = root ?: return copy(root = Leaf.of(tile), fullscreen = null).focus(tile)
    val target = focused?.let(root::leafOf) ?: root.leaves().last()
    val updated =
        if (layout == TilingLayout.Master) Split(Leaf.of(tile), root) else root.replaceLeaf(target, Split(target, Leaf.of(tile)))
    return copy(root = updated, fullscreen = null).focus(tile)
}

/**
 * Quita [tile] (cerrar la "ventana": nunca mata la app, plan.md §2.2). Su hueco lo ocupa el hermano.
 * Si tenía el foco, vuelve al último tile enfocado que siga existiendo; sin historial, al vecino
 * (la pestaña anterior del grupo, el hermano en el árbol o el último flotante).
 */
fun Workspace.remove(tile: TileId): Workspace {
    if (tile !in this) return this
    // Primero sin foco (el tile ya no existe y el constructor lo rechazaría) y después el vecino.
    val after =
        copy(
            root = if (isFloating(tile)) root else root?.withoutTile(tile),
            floating = floating.filterNot { it.id == tile },
            focused = focused.takeIf { it != tile },
            fullscreen = fullscreen.takeIf { it != tile },
            history = history - tile,
        )
    if (focused != tile) return after
    // `focus` también pone visible su pestaña si está en un grupo.
    return (after.history.lastOrNull() ?: nextFocus(tile, after))?.let(after::focus) ?: after
}

private fun Workspace.nextFocus(
    removed: TileId,
    after: Workspace,
): TileId? {
    val leaf = root?.leafOf(removed)
    return when {
        leaf != null && leaf.isGroup -> after.root?.leafOf(leaf.tiles.first { it != removed })?.shown
        leaf != null -> after.root?.let { siblingOf(leaf)?.leaves()?.first()?.shown ?: it.leaves().firstOrNull()?.shown }
        else ->
            after.root
                ?.leaves()
                ?.lastOrNull()
                ?.shown
    } ?: after.floating.lastOrNull()?.id
}

/** El hermano de [leaf] en el árbol actual (antes de quitarla). */
private fun Workspace.siblingOf(leaf: Leaf): Node? {
    fun search(node: Node): Node? =
        when (node) {
            is Leaf -> null
            is Split ->
                when {
                    node.first == leaf -> node.second
                    node.second == leaf -> node.first
                    else -> search(node.first) ?: search(node.second)
                }
        }
    return root?.let(::search)
}

/**
 * El árbol sin [tile]. En un grupo sólo sale de las pestañas: si era la visible, se ve la anterior
 * (la de su izquierda), que es la que se veía antes de añadirla.
 */
internal fun Node.withoutTile(tile: TileId): Node? {
    val leaf = checkNotNull(leafOf(tile))
    if (!leaf.isGroup) return removeLeaf(leaf)
    val index = leaf.tiles.indexOf(tile)
    val active =
        when {
            index == leaf.active -> (index - 1).coerceAtLeast(0)
            index < leaf.active -> leaf.active - 1
            else -> leaf.active
        }
    return replaceLeaf(leaf, Leaf(leaf.tiles - tile, active))
}

/** Intercambia dos tiles del mosaico (hueco por hueco, con sus grupos). Flotantes: sin cambios. */
fun Workspace.swap(
    a: TileId,
    b: TileId,
): Workspace {
    val root = root ?: return this
    val tiled = root.tiles()
    if (a == b || a !in tiled || b !in tiled) return this
    return copy(
        root =
            root.mapLeaves { leaf ->
                leaf.copy(
                    tiles =
                        leaf.tiles.map {
                            if (it == a) {
                                b
                            } else if (it == b) {
                                a
                            } else {
                                it
                            }
                        },
                )
            },
    )
}

private fun Node.mapLeaves(transform: (Leaf) -> Leaf): Node =
    when (this) {
        is Leaf -> transform(this)
        is Split -> copy(first = first.mapLeaves(transform), second = second.mapLeaves(transform))
    }

/**
 * `Super + T` (plan.md §3): el tile con el foco pasa de mosaico a flotante (centrado, 60 % del área)
 * o al revés (vuelve al mosaico como un tile nuevo). Conserva el foco.
 */
fun Workspace.toggleFloating(tile: TileId? = focused): Workspace {
    if (tile == null || tile !in this) return this
    return if (isFloating(tile)) {
        // Sin foco ni pantalla completa un instante (el tile no está en ningún sitio), y de vuelta al mosaico.
        copy(
            floating = floating.filterNot { it.id == tile },
            focused = focused.takeIf { it != tile },
            fullscreen = null,
            history =
                history - tile,
        ).insert(tile)
    } else {
        copy(root = root?.withoutTile(tile), floating = floating + FloatingTile(tile))
    }
}

/** Mueve o cambia de tamaño un flotante. */
fun Workspace.withFloatingBounds(
    tile: TileId,
    bounds: FloatingBounds,
): Workspace = copy(floating = floating.map { if (it.id == tile) it.copy(bounds = bounds) else it })

/** `Super + F`: el tile con el foco ocupa todo el workspace, o vuelve a su sitio. */
fun Workspace.toggleFullscreen(): Workspace = copy(fullscreen = if (fullscreen != null) null else focused)

/** Pasa a la pestaña siguiente (o anterior) del grupo con el foco, y la enfoca. */
fun Workspace.cycleGroup(forward: Boolean = true): Workspace {
    val leaf = focused?.let { root?.leafOf(it) } ?: return this
    if (!leaf.isGroup) return this
    val size = leaf.tiles.size
    val active = Math.floorMod(leaf.active + if (forward) 1 else -1, size)
    return focus(leaf.tiles[active])
}
