package com.wh01s17.launcher.feature.menu.search

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.designsystem.components.WhListRow
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.menu.R
import android.view.KeyEvent as AndroidKeyEvent

private class ContextRow(
    val title: String,
    val icon: WhGlyph,
    val intent: SearchIntent,
    val enabled: Boolean = true,
)

/** Diálogo modal: el teclado y TalkBack permanecen dentro del menú contextual. */
@Composable
internal fun AppMenuOverlay(
    menu: AppMenu,
    iconLoader: AppIconLoader,
    onIntent: (SearchIntent) -> Unit,
) {
    val rows =
        menu.shortcuts.map { ContextRow(it.shortLabel, WhIcons.ExternalLink, SearchIntent.LaunchShortcut(it), it.enabled) } +
            listOf(ContextRow(stringResource(R.string.drawer_action_info), WhIcons.Info, SearchIntent.OpenInfo(menu.app))) +
            if (menu.app.uninstallable) {
                listOf(ContextRow(stringResource(R.string.drawer_action_uninstall), WhIcons.Trash, SearchIntent.Uninstall(menu.app)))
            } else {
                emptyList()
            }
    var selected by remember(menu.app.key) { mutableIntStateOf(0) }
    val keyboard =
        ContextMenuKeys(
            move = { delta -> selected = Math.floorMod(selected + delta, rows.size) },
            activate = { rows.getOrNull(selected)?.takeIf { it.enabled }?.let { onIntent(it.intent) } },
            dismiss = { onIntent(SearchIntent.DismissMenu) },
        )
    val scroll = rememberScrollState()
    val rowPixels =
        with(androidx.compose.ui.platform.LocalDensity.current) {
            maxOf(WhTheme.spacing.touchTarget, WhTheme.spacing.rowHeight).roundToPx()
        }
    LaunchedEffect(selected) { scroll.animateScrollTo(selected * rowPixels) }
    val focus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = { onIntent(SearchIntent.DismissMenu) },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        DialogKeyboard { event ->
            keyboard.handle(event.keyCode, event.action == AndroidKeyEvent.ACTION_UP && !event.isCanceled, event.isShiftPressed)
        }
        LaunchedEffect(Unit) { focus.requestFocus() }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            WhScrim(onClick = { onIntent(SearchIntent.DismissMenu) }, clickLabel = stringResource(R.string.drawer_close))
            WhPanel(
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(WhTheme.spacing.controlGap)
                    .widthIn(max = WhTheme.spacing.menuWidth)
                    .focusRequester(focus)
                    .onPreviewKeyEvent { event ->
                        keyboard.handle(contextKeyCode(event.key), event.type == KeyEventType.KeyUp, event.isShiftPressed)
                    }.focusable(),
                contentPadding = WhTheme.spacing.controlGap,
            ) {
                DrawerIcon(menu.app, iconLoader)
                WhText(menu.app.label, color = WhTheme.colors.accent)
                Column(Modifier.verticalScroll(scroll)) {
                    rows.forEachIndexed { index, row ->
                        WhListRow(
                            row.title,
                            { onIntent(row.intent) },
                            leadingIcon = row.icon,
                            enabled = row.enabled,
                            selected = index == selected,
                            keyboardSelected = index == selected,
                        )
                    }
                    if (!menu.shortcutsAvailable) WhText(stringResource(R.string.drawer_shortcuts_unavailable))
                }
            }
        }
    }
}
