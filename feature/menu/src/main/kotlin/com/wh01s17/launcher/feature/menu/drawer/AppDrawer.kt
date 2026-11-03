package com.wh01s17.launcher.feature.menu.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.components.WhTextField
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.menu.R

/**
 * Drawer provisional de apps (plan.md §7, Hito 2): búsqueda arriba, lista debajo, menú
 * contextual al mantener pulsado. Sube desde abajo sobre un velo; se cierra con *atrás*, Escape,
 * tocando fuera o al abrir algo. Lo sustituye el menú wh01s17 en el Hito 5.
 */
@Composable
fun AppDrawer(
    state: AppDrawerState,
    iconLoader: AppIconLoader,
    onIntent: (DrawerIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // *Atrás* cierra primero el menú contextual y luego el drawer.
    BackHandler(enabled = state.open || state.menu != null) {
        onIntent(if (state.menu != null) DrawerIntent.DismissMenu else DrawerIntent.Close)
    }
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(visible = state.open, enter = fadeIn(), exit = fadeOut()) {
            WhScrim(onClick = { onIntent(DrawerIntent.Close) }, clickLabel = stringResource(R.string.drawer_close))
        }
        AnimatedVisibility(
            visible = state.open,
            enter = slideInVertically { it / SLIDE_FRACTION } + fadeIn(),
            exit = slideOutVertically { it / SLIDE_FRACTION } + fadeOut(),
        ) {
            DrawerPanel(state, iconLoader, onIntent)
        }
        state.menu?.let { menu -> AppMenuOverlay(menu, iconLoader, onIntent) }
    }
}

@Composable
private fun DrawerPanel(
    state: AppDrawerState,
    iconLoader: AppIconLoader,
    onIntent: (DrawerIntent) -> Unit,
) {
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { focus.requestFocus() }
    // Cada búsqueda nueva vuelve al primer resultado.
    LaunchedEffect(state.query) { listState.scrollToItem(0) }
    WhPanel(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(WhTheme.spacing.controlGap)
                .onPreviewKeyEvent { event ->
                    val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
                    if (escape) onIntent(DrawerIntent.Close)
                    escape
                },
        style = WhTheme.surfaces.launcher.surface,
        contentPadding = WhTheme.spacing.controlGap,
    ) {
        WhTextField(
            value = state.query,
            onValueChange = { onIntent(DrawerIntent.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            placeholder = stringResource(R.string.drawer_search_placeholder),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onIntent(DrawerIntent.LaunchFirst) }),
        )
        state.message?.let { DrawerMessageLine(it) }
        when {
            !state.loaded -> DrawerHint(stringResource(R.string.drawer_loading))
            state.items.isEmpty() -> DrawerHint(stringResource(R.string.drawer_empty))
            else ->
                LazyColumn(Modifier.fillMaxWidth().weight(1f), state = listState) {
                    items(state.items, key = { it.app.key.toString() }) { item ->
                        AppRow(item, iconLoader, onIntent)
                    }
                }
        }
    }
}

@Composable
private fun DrawerHint(text: String) {
    WhText(text, Modifier.padding(horizontal = WhTheme.spacing.rowPaddingX), color = WhTheme.colors.darkForeground)
}

@Composable
private fun DrawerMessageLine(message: DrawerMessage) {
    val colors = WhTheme.colors
    val (text, color) =
        when (message) {
            DrawerMessage.ProfilePaused -> stringResource(R.string.drawer_message_paused) to colors.yellow
            DrawerMessage.ProfileResumeRequested -> stringResource(R.string.drawer_message_paused_requested) to colors.yellow
            DrawerMessage.LaunchFailed -> stringResource(R.string.drawer_message_failed) to colors.red
        }
    Column(Modifier.padding(horizontal = WhTheme.spacing.rowPaddingX)) {
        WhText(text, style = WhTheme.typography.bodySmall, color = color)
    }
}

/** Cuánto sube el panel al abrirse: una fracción de su alto, no la pantalla entera. */
private const val SLIDE_FRACTION = 6
