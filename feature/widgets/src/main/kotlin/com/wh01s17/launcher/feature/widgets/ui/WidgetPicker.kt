package com.wh01s17.launcher.feature.widgets.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.wh01s17.launcher.core.designsystem.components.WhListRow
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.components.WhTextField
import com.wh01s17.launcher.core.designsystem.layout.chromeAndKeyboard
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.widgets.R
import com.wh01s17.launcher.feature.widgets.host.WidgetProvider
import com.wh01s17.launcher.feature.widgets.host.installedWidgets
import com.wh01s17.launcher.feature.widgets.native.NativeWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Selector de widgets (plan.md §7 Hito 7): los widgets propios y los instalados, por app, con el
 * tamaño mínimo que piden, y un buscador como el del menú. Es propio, con el tema wh01s17: el
 * selector del sistema no existe en todos los fabricantes. La lista se lee fuera del hilo principal.
 */
@Composable
fun WidgetPicker(
    onPick: (WidgetProvider) -> Unit,
    onPickNative: (NativeWidget) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val providers by produceState<List<WidgetProvider>?>(null) { value = withContext(Dispatchers.IO) { installedWidgets(context) } }
    val nativeApp = stringResource(R.string.native_section)
    val nativeLabels = NativeWidget.entries.map { stringResource(it.label) }
    val entries =
        remember(providers, nativeLabels, nativeApp) {
            NativeWidget.entries.mapIndexed { i, widget -> PickerEntry.Native(widget, nativeLabels[i], nativeApp) } +
                providers.orEmpty().map { PickerEntry.System(it) }
        }
    val search = remember(entries) { WidgetSearch(entries, PickerEntry::label, PickerEntry::app) }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(search, query) { if (query.isBlank()) null else search.search(query) }
    val pick: (PickerEntry) -> Unit = { entry ->
        when (entry) {
            is PickerEntry.Native -> onPickNative(entry.widget)
            is PickerEntry.System -> onPick(entry.provider)
        }
    }
    val spacing = WhTheme.spacing
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WhScrim(onClick = onDismiss, clickLabel = stringResource(R.string.widget_picker_close))
        WhPanel(
            Modifier
                .windowInsetsPadding(WindowInsets.chromeAndKeyboard)
                .padding(spacing.panelPadding)
                .widthIn(max = spacing.menuWidth * 2)
                .heightIn(max = spacing.menuWidth * 3),
        ) {
            WhText(stringResource(R.string.widget_picker_title), style = WhTheme.typography.title, color = WhTheme.colors.accent)
            WhTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                placeholder = stringResource(R.string.widget_picker_search),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onGo = { results?.firstOrNull()?.let(pick) }),
            )
            if (results == null) GroupedList(entries, pick) else ResultList(results, pick)
        }
    }
}

/** Sin consulta: por app, su nombre como cabecera y debajo sus widgets; los propios primero. */
@Composable
private fun GroupedList(
    entries: List<PickerEntry>,
    pick: (PickerEntry) -> Unit,
) {
    val groups = entries.groupBy { it.app }
    LazyColumn(Modifier.fillMaxWidth()) {
        for ((app, widgets) in groups) {
            item(key = "app:$app") { Header(app) }
            items(widgets, key = ::entryKey) { entry -> Row(entry, trailing = sizeOf(entry), pick) }
        }
    }
}

/** Con consulta: de mejor a peor, con la app al lado (ya no hay cabeceras que la digan). */
@Composable
private fun ResultList(
    results: List<PickerEntry>,
    pick: (PickerEntry) -> Unit,
) {
    if (results.isEmpty()) {
        WhText(
            stringResource(R.string.widget_picker_none),
            Modifier.padding(top = WhTheme.spacing.panelGap),
            color = WhTheme.colors.darkForeground,
        )
        return
    }
    LazyColumn(Modifier.fillMaxWidth()) {
        items(results, key = ::entryKey) { entry -> Row(entry, trailing = entry.app, pick) }
    }
}

@Composable
private fun Row(
    entry: PickerEntry,
    trailing: String?,
    pick: (PickerEntry) -> Unit,
) {
    WhListRow(title = entry.label, onClick = { pick(entry) }, trailingText = trailing)
}

@Composable
private fun sizeOf(entry: PickerEntry): String? =
    (entry as? PickerEntry.System)?.provider?.let { stringResource(R.string.widget_picker_size, it.minWidthDp, it.minHeightDp) }

private fun entryKey(entry: PickerEntry): String =
    when (entry) {
        is PickerEntry.Native -> "native:${entry.widget.id}"
        is PickerEntry.System -> "${entry.provider.info.provider.flattenToShortString()}@${entry.provider.info.profile.hashCode()}"
    }

@Composable
private fun Header(text: String) {
    WhText(
        "-- $text --",
        Modifier.padding(top = WhTheme.spacing.panelGap),
        style = WhTheme.typography.caption,
        color = WhTheme.colors.darkForeground,
    )
}
