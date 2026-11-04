package com.wh01s17.launcher.feature.workspaces.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.binding.Gesture
import com.wh01s17.launcher.core.tiling.LayoutResult
import com.wh01s17.launcher.core.tiling.Rect
import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.core.tiling.Workspace
import com.wh01s17.launcher.core.tiling.layout
import com.wh01s17.launcher.feature.workspaces.R
import com.wh01s17.launcher.feature.workspaces.WorkspacesIntent
import com.wh01s17.launcher.feature.workspaces.WorkspacesUiState
import com.wh01s17.launcher.feature.workspaces.model.Desk
import kotlin.math.abs

/** Capas del lienzo: tiles, flotantes encima, y lo que se arrastra encima de todo. */
private const val Z_FLOATING = 1f
private const val Z_EDIT = 2f
private const val Z_DRAG = 3f

/** Opacidad del tile que se arrastra, para ver dónde cae. */
private const val DRAG_ALPHA = 0.85f

/**
 * Un escritorio (plan.md §7 Hito 4): los tiles donde los pone el motor de tiling. Fuera del modo
 * edición, tocar un tile abre su app, mantenerlo entra en edición y deslizar hacia arriba abre el
 * menú. En edición, tocar enfoca, arrastrar un tile lo intercambia o lo lleva a otro escritorio y
 * arrastrar un borde cambia el tamaño.
 */
@Composable
internal fun WorkspacePage(
    workspace: Workspace,
    number: Int,
    state: WorkspacesUiState,
    style: TilingStyle,
    iconLoader: AppIconLoader,
    onIntent: (WorkspacesIntent) -> Unit,
    onOpenMenu: () -> Unit,
    onGesture: (Gesture) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editing = state.editing
    BoxWithConstraints(modifier.fillMaxSize().then(if (editing) Modifier else Modifier.verticalSwipes(onGesture))) {
        val area = Rect(0, 0, constraints.maxWidth, constraints.maxHeight)
        val gaps = style.gaps(LocalDensity.current)
        val result = remember(workspace, area, gaps) { layout(workspace, area, gaps) }
        var drag by remember { mutableStateOf<TileDrag?>(null) }
        val stripHeight = with(LocalDensity.current) { WhTheme.spacing.touchTarget.roundToPx() }
        val canvas = PageCanvas(area, area.inset(gaps.outer), stripHeight, number)
        Background(editing, onIntent, onOpenMenu)
        if (workspace.isEmpty) EmptyHint(number, Modifier.align(Alignment.Center))
        for ((tile, rect) in result.visible) {
            key(tile) {
                Tile(tile, rect, workspace, result, state, style, iconLoader, canvas, drag, { drag = it }, onIntent)
            }
        }
        if (editing) DividerHandles(workspace, area, gaps, onIntent, Modifier.zIndex(Z_EDIT))
        drag?.let { current ->
            val preview = current.preview(canvas.outer, stripHeight)
            TileView(
                info = state.tiles[current.tile],
                appsLoaded = state.appsLoaded,
                focused = true,
                tabs = null,
                shown = current.tile,
                style = style,
                iconLoader = iconLoader,
                onSelectTab = {},
                modifier = Modifier.placeAt(preview).zIndex(Z_DRAG).alpha(DRAG_ALPHA),
            )
            if (!current.resize) {
                val hovered = stripCellAt(current.pointer, area.width, stripHeight, Desk.COUNT)
                DropStrip(number, hovered, Modifier.placeAt(Rect(0, 0, area.width, stripHeight)).zIndex(Z_DRAG))
            }
        }
    }
}

/** Lo que un tile necesita saber del lienzo para soltarse. */
internal class PageCanvas(
    val area: Rect,
    val outer: Rect,
    val stripHeight: Int,
    val number: Int,
)

@Composable
private fun Tile(
    tile: TileId,
    rect: Rect,
    workspace: Workspace,
    result: LayoutResult,
    state: WorkspacesUiState,
    style: TilingStyle,
    iconLoader: AppIconLoader,
    canvas: PageCanvas,
    drag: TileDrag?,
    onDrag: (TileDrag?) -> Unit,
    onIntent: (WorkspacesIntent) -> Unit,
) {
    val floating = tile in result.floating
    val dragging = drag?.tile == tile
    val gestures = rememberTileGestures(tile, rect, floating, result, canvas, state.editing, onDrag, onIntent)
    Box(Modifier.placeAt(rect).zIndex(if (floating) Z_FLOATING else 0f).then(gestures)) {
        if (dragging) {
            TileGhost(style)
        } else {
            TileView(
                info = state.tiles[tile],
                appsLoaded = state.appsLoaded,
                focused = tile == workspace.focused,
                tabs = result.groups[tile]?.map { it to state.tiles[it] },
                shown = tile,
                style = style,
                iconLoader = iconLoader,
                onSelectTab = { onIntent(WorkspacesIntent.SelectTab(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (state.editing && floating && !dragging) {
            ResizeCorner(tile, rect, canvas, onDrag, onIntent, Modifier.align(Alignment.BottomEnd))
        }
    }
}

/**
 * Toques de un tile. Fuera de edición: tocar abre (la ventana crece desde el tile), mantener entra
 * en edición. En edición: tocar enfoca y arrastrar mueve el tile.
 */
@Composable
private fun rememberTileGestures(
    tile: TileId,
    rect: Rect,
    floating: Boolean,
    result: LayoutResult,
    canvas: PageCanvas,
    editing: Boolean,
    onDrag: (TileDrag?) -> Unit,
    onIntent: (WorkspacesIntent) -> Unit,
): Modifier {
    val view = LocalView.current
    var bounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val latestRect by rememberUpdatedState(rect)
    val latestFloating by rememberUpdatedState(floating)
    val latestResult by rememberUpdatedState(result)
    val latestCanvas by rememberUpdatedState(canvas)
    val openLabel = stringResource(R.string.tile_open)
    val editLabel = stringResource(R.string.tile_edit)
    val tap = { onIntent(WorkspacesIntent.TapTile(tile, bounds?.let { LaunchOrigin(view, it.toAndroidRect()) })) }
    val longPress = { onIntent(WorkspacesIntent.LongPressTile(tile)) }
    var current by remember { mutableStateOf<TileDrag?>(null) }
    val base =
        Modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .semantics(mergeDescendants = true) {
                onClick(openLabel) {
                    tap()
                    true
                }
                if (!editing) {
                    onLongClick(editLabel) {
                        longPress()
                        true
                    }
                }
            }.pointerInput(tile, editing) {
                detectTapGestures(onTap = { tap() }, onLongPress = if (editing) null else { _ -> longPress() })
            }
    if (!editing) return base
    return base.pointerInput(tile) {
        detectDragGestures(
            onDragStart = { touch ->
                val r = latestRect
                current = TileDrag(tile, r, latestFloating, resize = false, start = Offset(r.left + touch.x, r.top + touch.y))
                onDrag(current)
            },
            onDragEnd = {
                current?.let { drop(it, latestResult, latestCanvas, onIntent) }
                current = null
                onDrag(null)
            },
            onDragCancel = {
                current = null
                onDrag(null)
            },
        ) { change, amount ->
            change.consume()
            current = current?.let { it.copy(offset = it.offset + amount) }
            onDrag(current)
        }
    }
}

/** Soltar: sobre la tira de escritorios lo lleva allí; un flotante se queda donde cae; un tile, cambia con el de debajo. */
private fun drop(
    drag: TileDrag,
    result: LayoutResult,
    canvas: PageCanvas,
    onIntent: (WorkspacesIntent) -> Unit,
) {
    val cell = if (drag.resize) null else stripCellAt(drag.pointer, canvas.area.width, canvas.stripHeight, Desk.COUNT)
    when {
        cell != null -> if (cell != canvas.number) onIntent(WorkspacesIntent.DropOnWorkspace(drag.tile, cell))
        drag.floating ->
            floatingBoundsOf(drag.preview(canvas.outer, canvas.stripHeight), canvas.outer)?.let {
                onIntent(WorkspacesIntent.MoveFloating(drag.tile, it))
            }
        else ->
            result.tiled.entries
                .firstOrNull { (other, rect) -> other != drag.tile && drag.pointer in rect }
                ?.let { onIntent(WorkspacesIntent.SwapTiles(drag.tile, it.key)) }
    }
}

/** Esquina para redimensionar un flotante en el modo edición. */
@Composable
private fun ResizeCorner(
    tile: TileId,
    rect: Rect,
    canvas: PageCanvas,
    onDrag: (TileDrag?) -> Unit,
    onIntent: (WorkspacesIntent) -> Unit,
    modifier: Modifier,
) {
    val latestRect by rememberUpdatedState(rect)
    val latestCanvas by rememberUpdatedState(canvas)
    var current by remember { mutableStateOf<TileDrag?>(null) }
    ResizeGrip(
        modifier.pointerInput(tile) {
            detectDragGestures(
                onDragStart = { touch ->
                    val r = latestRect
                    current = TileDrag(tile, r, floating = true, resize = true, start = Offset(r.right + touch.x, r.bottom + touch.y))
                    onDrag(current)
                },
                onDragEnd = {
                    current?.let { d ->
                        floatingBoundsOf(d.preview(latestCanvas.outer, latestCanvas.stripHeight), latestCanvas.outer)?.let {
                            onIntent(WorkspacesIntent.MoveFloating(tile, it))
                        }
                    }
                    current = null
                    onDrag(null)
                },
                onDragCancel = {
                    current = null
                    onDrag(null)
                },
            ) { change, amount ->
                change.consume()
                current = current?.let { it.copy(offset = it.offset + amount) }
                onDrag(current)
            }
        },
    )
}

/** El fondo: tocarlo abre el menú (o sale de edición); mantenerlo entra en edición. */
@Composable
private fun Background(
    editing: Boolean,
    onIntent: (WorkspacesIntent) -> Unit,
    onOpenMenu: () -> Unit,
) {
    val openLabel = stringResource(R.string.workspace_open_menu)
    val editLabel = stringResource(R.string.workspace_edit)
    Box(
        Modifier
            .fillMaxSize()
            .semantics {
                onClick(openLabel) {
                    onOpenMenu()
                    true
                }
                onLongClick(editLabel) {
                    onIntent(WorkspacesIntent.LongPressBackground)
                    true
                }
            }.pointerInput(editing) {
                detectTapGestures(
                    onTap = { if (editing) onIntent(WorkspacesIntent.ExitEdit) else onOpenMenu() },
                    onLongPress = { onIntent(WorkspacesIntent.LongPressBackground) },
                )
            },
    )
}

@Composable
private fun EmptyHint(
    number: Int,
    modifier: Modifier,
) {
    val spacing = WhTheme.spacing
    Column(
        modifier.padding(spacing.panelPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.controlGap),
    ) {
        WhText(
            stringResource(R.string.workspace_empty_title, workspaceLabel(number)),
            style = WhTheme.typography.subtitle,
            color = WhTheme.colors.darkForeground,
        )
        WhText(
            stringResource(R.string.workspace_empty_hint),
            style = WhTheme.typography.caption,
            color = WhTheme.colors.darkForeground,
            textAlign = TextAlign.Center,
        )
    }
}

/** El escritorio 10 se llama «0», como la tecla `Super + 0` y la barra. */
internal fun workspaceLabel(number: Int): String = if (number == Desk.LAST) "0" else number.toString()

/**
 * Deslizar en vertical emite [Gesture.SwipeUp] o [Gesture.SwipeDown]; qué hacen lo deciden los
 * `bindings` (de fábrica, hacia arriba abre el menú, plan.md §3). Va en el contenedor, no en el
 * fondo: así también funciona empezando sobre un tile (los tiles no consumen el movimiento fuera de
 * edición). Basta con recorrer una fila del menú; un mismo arrastre emite un solo gesto.
 */
@Composable
private fun Modifier.verticalSwipes(onGesture: (Gesture) -> Unit): Modifier {
    val threshold = with(LocalDensity.current) { WhTheme.spacing.menuRowHeight.toPx() }
    val latest by rememberUpdatedState(onGesture)
    return pointerInput(threshold) {
        var travelled = 0f
        var fired = false
        detectVerticalDragGestures(
            onDragStart = {
                travelled = 0f
                fired = false
            },
            onVerticalDrag = { change, dragAmount ->
                travelled += dragAmount
                if (!fired && abs(travelled) > threshold) {
                    change.consume()
                    fired = true
                    latest(if (travelled < 0) Gesture.SwipeUp else Gesture.SwipeDown)
                }
            },
        )
    }
}
