package com.wh01s17.launcher.feature.menu.panel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.designsystem.components.WhListRow
import com.wh01s17.launcher.core.designsystem.components.WhListRowSize
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.components.WhTextField
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.menu.R
import com.wh01s17.launcher.feature.menu.search.AppMenuOverlay
import com.wh01s17.launcher.feature.menu.search.MenuSearchState
import com.wh01s17.launcher.feature.menu.search.SearchIntent
import com.wh01s17.launcher.feature.menu.search.SearchResults

/** Un único panel para navegación, búsqueda global y selección de apps para tiles. */
@Composable
fun LauncherMenu(
    page: MenuPage?,
    onNavigate: (MenuPage) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onAction: (MenuAction) -> Unit,
    modifier: Modifier = Modifier,
    search: MenuSearchState = MenuSearchState(),
    iconLoader: AppIconLoader? = null,
    onSearch: (SearchIntent) -> Unit = {},
    version: String = "",
) {
    if (page == null) return
    val focus = remember { FocusRequester() }
    LaunchedEffect(page) { focus.requestFocus() }
    var text by androidx.compose.runtime.saveable.rememberSaveable(
        search.inputEpoch,
    ) { androidx.compose.runtime.mutableStateOf(search.query) }
    val currentSearch =
        search.copy(
            query = text,
            results = if (text == search.query) search.results else emptyList(),
            loaded = search.loaded && text == search.query,
        )
    var selected by remember(page, text) { mutableIntStateOf(0) }
    val held = remember { mutableSetOf<Key>() }
    val navigation =
        MenuNavigation(page, currentSearch, selected, { selected = it }, MenuCallbacks(onNavigate, onBack, onAction, onSearch), held)
    BackHandler(onBack = navigation::back)
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WhScrim(onClick = onClose, clickLabel = stringResource(R.string.drawer_close))
        WhPanel(
            Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(WhTheme.spacing.controlGap)
                .widthIn(max = WhTheme.spacing.menuWidth)
                .fillMaxWidth()
                .onPreInterceptKeyBeforeSoftKeyboard(navigation::onKey)
                .onPreviewKeyEvent(navigation::onKey)
                .pointerInput(Unit) { detectTapGestures {} },
            contentPadding = WhTheme.spacing.controlGap,
        ) {
            WhText(stringResource(page.title), style = WhTheme.typography.heading, color = WhTheme.colors.accent)
            MenuSearchInput(
                text,
                search.picking,
                focus,
                onValueChange = { value ->
                    text = value
                    onSearch(SearchIntent.QueryChanged(value))
                },
                activate = {
                    if (text.isNotEmpty() || page == MenuPage.Apps) onSearch(SearchIntent.LaunchFirst) else navigation.activate()
                },
            )
            if (navigation.searching) {
                SearchResults(currentSearch, iconLoader, onSearch, Modifier.weight(1f, fill = false))
            } else {
                MenuPageContent(page, menuEntries(page), selected, onBack, onNavigate, onAction, version, Modifier.weight(1f, fill = false))
            }
        }
        if (iconLoader != null) search.menu?.let { AppMenuOverlay(it, iconLoader, onSearch) }
    }
}

@Composable
private fun MenuPageContent(
    page: MenuPage,
    entries: List<MenuEntry>,
    selected: Int,
    onBack: () -> Unit,
    onNavigate: (MenuPage) -> Unit,
    onAction: (MenuAction) -> Unit,
    version: String,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val rowPixels = with(androidx.compose.ui.platform.LocalDensity.current) { WhTheme.spacing.menuRowHeight.roundToPx() }
    LaunchedEffect(selected, page) { scroll.animateScrollTo(selected * rowPixels) }
    Column(modifier.verticalScroll(scroll)) {
        if (page != MenuPage.Root) WhListRow(stringResource(R.string.menu_back), onBack, leadingIcon = WhIcons.ChevronLeft)
        menuDescription(page)?.let { WhText(stringResource(it), Modifier.padding(WhTheme.spacing.rowPaddingX)) }
        if (page == MenuPage.Update || page == MenuPage.About) WhText(stringResource(R.string.menu_version, version))
        entries.forEachIndexed { index, entry ->
            WhListRow(
                title = stringResource(entry.title),
                onClick = {
                    entry.page?.let(onNavigate)
                    entry.action?.let(onAction)
                },
                selected = index == selected,
                keyboardSelected = index == selected,
                leadingIcon = entry.icon,
                chevron = entry.page != null,
                size = WhListRowSize.Menu,
            )
        }
    }
}

/** La edición responde en el mismo frame, aunque el índice esté ocupado en otro dispatcher. */
@Composable
private fun MenuSearchInput(
    text: String,
    picking: Boolean,
    focus: FocusRequester,
    onValueChange: (String) -> Unit,
    activate: () -> Unit,
) {
    WhTextField(
        text,
        onValueChange,
        Modifier.fillMaxWidth().focusRequester(focus),
        placeholder = stringResource(if (picking) R.string.drawer_pick_placeholder else R.string.menu_search_placeholder),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, showKeyboardOnFocus = false, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onGo = { activate() }),
    )
}
