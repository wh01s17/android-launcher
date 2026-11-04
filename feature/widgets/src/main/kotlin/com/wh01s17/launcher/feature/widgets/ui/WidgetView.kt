package com.wh01s17.launcher.feature.widgets.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.widgets.R
import com.wh01s17.launcher.feature.widgets.host.WidgetHost

/**
 * Un widget del sistema dentro de un tile. Si su app ya no está, se ve un aviso atenuado; el tile
 * se conserva, como el de una app desinstalada (plan.md §11.1).
 */
@Composable
fun WidgetView(
    host: WidgetHost,
    appWidgetId: Int,
    interactive: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = remember(appWidgetId) { host.info(appWidgetId) != null }
    if (!available) {
        Box(modifier, contentAlignment = Alignment.Center) {
            WhText(
                stringResource(R.string.widget_unavailable),
                style = WhTheme.typography.bodySmall,
                color = WhTheme.colors.darkForeground,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val latestLongPress by rememberUpdatedState(onLongPress)
    key(appWidgetId) {
        AndroidView(
            factory = { context -> host.createView(context, appWidgetId) },
            update = { view ->
                view.interactive = interactive
                view.onLongPress = { latestLongPress() }
            },
            modifier = modifier,
        )
    }
}
