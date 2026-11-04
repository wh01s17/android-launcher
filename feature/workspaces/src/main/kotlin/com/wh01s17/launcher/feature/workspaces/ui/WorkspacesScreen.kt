package com.wh01s17.launcher.feature.workspaces.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.input.binding.Gesture
import com.wh01s17.launcher.core.tiling.Rect
import com.wh01s17.launcher.feature.workspaces.CanvasMetrics
import com.wh01s17.launcher.feature.workspaces.R
import com.wh01s17.launcher.feature.workspaces.WorkspaceMessage
import com.wh01s17.launcher.feature.workspaces.WorkspacesIntent
import com.wh01s17.launcher.feature.workspaces.WorkspacesUiState
import com.wh01s17.launcher.feature.workspaces.model.Desk

/** Lo menos que se encoge el escritorio en el modo edición, aunque la barra de herramientas crezca. */
private const val MIN_EDIT_SCALE = 0.5f

/** Se encoge hacia arriba y al centro, pegado a la barra. */
private const val EDIT_PIVOT_X = 0.5f

/**
 * Los escritorios (plan.md §2.1 y §7 Hito 4): un pager horizontal con un workspace por página que
 * se desliza con la animación `slide` de Hyprland (`easeOutQuint`, `speed 2`). [modifier] debe dejar
 * fuera la barra y los insets: el área que queda es la que reparte el motor de tiling.
 *
 * En el modo edición el escritorio se encoge entero para dejar sitio a la barra de herramientas:
 * así la distribución es exactamente la misma (un lienzo más bajo podría girar un split automático
 * y reordenar los tiles justo cuando se van a mover).
 */
@Composable
fun WorkspacesScreen(
    state: WorkspacesUiState,
    iconLoader: AppIconLoader,
    onIntent: (WorkspacesIntent) -> Unit,
    onCommand: (WorkspaceCommand) -> Unit,
    onAddTile: () -> Unit,
    onOpenMenu: () -> Unit,
    onGesture: (Gesture) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.editing) { onIntent(WorkspacesIntent.ExitEdit) }
    MessageToast(state.message, onIntent)
    var canvasHeight by remember { mutableIntStateOf(0) }
    var toolbarHeight by remember { mutableIntStateOf(0) }
    val editScale =
        if (state.editing && canvasHeight > 0) (1f - toolbarHeight.toFloat() / canvasHeight).coerceIn(MIN_EDIT_SCALE, 1f) else 1f
    val scale by animateFloatAsState(editScale, label = "editScale")
    Box(modifier.fillMaxSize().onSizeChanged { canvasHeight = it.height }) {
        if (state.loaded) {
            WorkspacePager(
                state,
                iconLoader,
                onIntent,
                onOpenMenu,
                onGesture,
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(EDIT_PIVOT_X, 0f)
                },
            )
        }
        AnimatedVisibility(
            visible = state.editing && state.loaded,
            modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { toolbarHeight = it.height },
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            EditToolbar(
                workspace = state.workspaces[state.current - Desk.FIRST],
                onCommand = onCommand,
                onAddTile = onAddTile,
                onOpenSettings = onOpenSettings,
                onDone = { onIntent(WorkspacesIntent.ExitEdit) },
            )
        }
    }
}

@Composable
private fun WorkspacePager(
    state: WorkspacesUiState,
    iconLoader: AppIconLoader,
    onIntent: (WorkspacesIntent) -> Unit,
    onOpenMenu: () -> Unit,
    onGesture: (Gesture) -> Unit,
    modifier: Modifier,
) {
    val style = rememberTilingStyle(state.general)
    val pager = rememberPagerState(initialPage = state.current - Desk.FIRST) { Desk.COUNT }
    val spec = remember(state.animation) { state.animation.spec() }
    val latestOnIntent by rememberUpdatedState(onIntent)
    // Un cambio de escritorio que no viene del dedo (barra, atajo) anima el pager hasta él…
    LaunchedEffect(state.current) {
        val target = state.current - Desk.FIRST
        if (pager.currentPage != target || pager.currentPageOffsetFraction != 0f) pager.animateScrollToPage(target, animationSpec = spec)
    }
    // …y uno que viene del dedo se guarda como el actual cuando el pager se para.
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.collect { latestOnIntent(WorkspacesIntent.PageSettled(it + Desk.FIRST)) }
    }
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(size, style, density) {
        if (size != IntSize.Zero) {
            latestOnIntent(
                WorkspacesIntent.CanvasChanged(CanvasMetrics(Rect(0, 0, size.width, size.height), style.gaps(density), density.density)),
            )
        }
    }
    HorizontalPager(
        state = pager,
        modifier = modifier.onSizeChanged { size = it },
        userScrollEnabled = !state.editing,
        flingBehavior = PagerDefaults.flingBehavior(state = pager, snapAnimationSpec = spec),
        key = { it },
    ) { page ->
        WorkspacePage(state.workspaces[page], page + Desk.FIRST, state, style, iconLoader, onIntent, onOpenMenu, onGesture)
    }
}

@Composable
private fun MessageToast(
    message: WorkspaceMessage?,
    onIntent: (WorkspacesIntent) -> Unit,
) {
    val context = LocalContext.current
    val text =
        message?.let {
            stringResource(
                when (it) {
                    WorkspaceMessage.NotInstalled -> R.string.message_not_installed
                    WorkspaceMessage.ProfilePaused -> R.string.message_paused
                    WorkspaceMessage.ProfileResumeRequested -> R.string.message_paused_requested
                    WorkspaceMessage.LaunchFailed -> R.string.message_failed
                },
            )
        }
    LaunchedEffect(message) {
        if (text != null) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            onIntent(WorkspacesIntent.MessageShown)
        }
    }
}
