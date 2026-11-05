package com.wh01s17.launcher.feature.workspaces.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.modifier.whBorder
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.workspaces.R
import com.wh01s17.launcher.feature.workspaces.WorkspacesIntent
import com.wh01s17.launcher.feature.workspaces.WorkspacesUiState
import com.wh01s17.launcher.feature.workspaces.model.Desk

/**
 * El scratchpad (plan.md §7 Hito 8): el `special:scratchpad` de Hyprland encima del escritorio
 * actual, que se atenúa. Baja desde arriba como `specialWorkspace` en `looknfeel.lua`
 * (`slidevert`, `speed 3` = 300 ms, `easeOutQuint`). Dentro funciona como cualquier escritorio:
 * abrir apps, modo edición, arrastrar (también a la tira de escritorios para sacar un tile).
 * Tocar fuera o *atrás* lo oculta.
 */
@Composable
internal fun ScratchpadOverlay(
    state: WorkspacesUiState,
    iconLoader: AppIconLoader,
    onIntent: (WorkspacesIntent) -> Unit,
    onOpenMenu: () -> Unit,
    gestures: PageGestures,
) {
    val easing = CubicBezierEasing(EASE_X1, EASE_Y1, EASE_X2, EASE_Y2)
    AnimatedVisibility(
        visible = state.loaded && state.scratchpadVisible,
        enter = fadeIn(tween(DURATION_MILLIS)),
        exit = fadeOut(tween(DURATION_MILLIS)),
    ) {
        // Con el modo edición abierto, *atrás* sale primero de la edición.
        BackHandler(enabled = !state.editing && transition.targetState == EnterExitState.Visible) {
            onIntent(WorkspacesIntent.HideScratchpad)
        }
        Box(Modifier.fillMaxSize()) {
            WhScrim(onClick = { onIntent(WorkspacesIntent.HideScratchpad) }, clickLabel = stringResource(R.string.scratchpad_hide))
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(WhTheme.spacing.panelPadding)
                    .animateEnterExit(
                        enter = slideInVertically(tween(DURATION_MILLIS, easing = easing)) { -it },
                        exit = slideOutVertically(tween(DURATION_MILLIS, easing = easing)) { -it },
                    ).background(WhTheme.colors.background, WhTheme.borders.shape)
                    .whBorder(active = true, shape = WhTheme.borders.shape),
            ) {
                val style = rememberTilingStyle(state.general)
                WorkspacePage(state.scratchpad, Desk.SCRATCHPAD, state, style, iconLoader, onIntent, onOpenMenu, gestures)
            }
        }
    }
}

// `easeOutQuint` de `looknfeel.lua` y `speed 3` (décimas de segundo).
private const val EASE_X1 = 0.23f
private const val EASE_Y1 = 1f
private const val EASE_X2 = 0.32f
private const val EASE_Y2 = 1f
private const val DURATION_MILLIS = 300
