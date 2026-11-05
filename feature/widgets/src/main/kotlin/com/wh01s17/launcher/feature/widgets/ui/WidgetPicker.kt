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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhListRow
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.chromeAndKeyboard
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.widgets.R
import com.wh01s17.launcher.feature.widgets.host.WidgetProvider
import com.wh01s17.launcher.feature.widgets.host.installedWidgets
import com.wh01s17.launcher.feature.widgets.native.NativeWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Selector de widgets (plan.md §7 Hito 7): los widgets instalados, por app, con el tamaño mínimo
 * que piden. Es propio, con el tema wh01s17: el selector del sistema no existe en todos los
 * fabricantes. La lista se lee fuera del hilo principal.
 */
@Composable
fun WidgetPicker(
    onPick: (WidgetProvider) -> Unit,
    onPickNative: (NativeWidget) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val providers by produceState<List<WidgetProvider>?>(null) { value = withContext(Dispatchers.IO) { installedWidgets(context) } }
    val spacing = WhTheme.spacing
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WhScrim(onClick = onDismiss, clickLabel = stringResource(R.string.widget_picker_close))
        WhPanel(
            Modifier
                .windowInsetsPadding(
                    WindowInsets.chromeAndKeyboard,
                ).padding(spacing.panelPadding)
                .widthIn(max = spacing.menuWidth * 2)
                .heightIn(
                    max =
                        spacing.menuWidth * 3,
                ),
        ) {
            WhText(stringResource(R.string.widget_picker_title), style = WhTheme.typography.title, color = WhTheme.colors.accent)
            ProviderList(providers.orEmpty(), onPick, onPickNative)
        }
    }
}

/** Por app: su nombre como cabecera y debajo sus widgets con el tamaño mínimo que piden. */
@Composable
private fun ProviderList(
    providers: List<WidgetProvider>,
    onPick: (WidgetProvider) -> Unit,
    onPickNative: (NativeWidget) -> Unit,
) {
    val groups = providers.groupBy { it.appLabel }
    LazyColumn(Modifier.fillMaxWidth()) {
        item(key = "native") { Header(stringResource(R.string.native_section)) }
        items(NativeWidget.entries, key = { "native:${it.id}" }) { widget ->
            WhListRow(title = stringResource(widget.label), onClick = { onPickNative(widget) })
        }
        for ((app, widgets) in groups) {
            item(key = "app:$app") { Header(app) }
            items(widgets, key = { "${it.info.provider.flattenToShortString()}@${it.info.profile.hashCode()}" }) { provider ->
                WhListRow(
                    title = provider.label,
                    onClick = { onPick(provider) },
                    trailingText = stringResource(R.string.widget_picker_size, provider.minWidthDp, provider.minHeightDp),
                )
            }
        }
    }
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
