package com.wh01s17.launcher.feature.menu.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.designsystem.components.WhListRow
import com.wh01s17.launcher.core.designsystem.components.WhListRowSize
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.menu.R

@Composable
internal fun SearchResults(
    state: MenuSearchState,
    iconLoader: AppIconLoader?,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = rememberLazyListState()
    LaunchedEffect(state.query, state.selected, state.results.size) {
        if (state.results.isNotEmpty()) list.animateScrollToItem(state.selected)
    }
    Column(modifier) {
        state.message?.let {
            val message =
                when (it) {
                    DrawerMessage.ProfilePaused -> R.string.drawer_message_paused
                    DrawerMessage.ProfileResumeRequested -> R.string.drawer_message_paused_requested
                    DrawerMessage.LaunchFailed -> R.string.drawer_message_failed
                }
            WhText(stringResource(message), Modifier.padding(WhTheme.spacing.rowPaddingX), color = WhTheme.colors.yellow)
        }
        if (!state.historyAvailable) {
            WhText(stringResource(R.string.menu_history_unavailable), color = WhTheme.colors.yellow)
        }
        if (state.results.isEmpty()) {
            WhText(stringResource(if (state.loaded) R.string.drawer_empty else R.string.drawer_loading))
        }
        LazyColumn(Modifier.fillMaxWidth(), state = list) {
            itemsIndexed(state.results, key = { _, result -> result.target.id }) { index, result ->
                val target = result.target
                if (target is SearchTarget.App && iconLoader != null) {
                    AppRow(DrawerItem(target.app, result.positions, result.badge), iconLoader, onIntent, selected = index == state.selected)
                } else {
                    val icon = if (target is SearchTarget.Command) target.icon else WhIcons.ExternalLink
                    WhListRow(
                        title = highlighted(target.label, result.positions, WhTheme.colors.accent),
                        onClick = { onIntent(SearchIntent.Activate(target)) },
                        selected = index == state.selected,
                        keyboardSelected = index == state.selected,
                        leading = { tint ->
                            com.wh01s17.launcher.core.designsystem.components
                                .WhIcon(icon, tint = tint)
                        },
                        size = WhListRowSize.Menu,
                    )
                }
            }
        }
    }
}
