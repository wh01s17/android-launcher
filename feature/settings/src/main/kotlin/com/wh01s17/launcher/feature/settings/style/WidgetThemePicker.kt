package com.wh01s17.launcher.feature.settings.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.config.LauncherConfig
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.designsystem.widgets.WidgetTheme
import com.wh01s17.launcher.feature.settings.R

/** Estilo → Widgets (`appearance.widgetTheme`): la temática actual, que abre la lista de todas. */
@Composable
internal fun WidgetThemeSetting(
    config: LauncherConfig,
    onChange: (String, String, Any) -> Unit,
) {
    ChoiceSetting(
        label = R.string.style_widgets_current,
        title = R.string.style_widgets_choose,
        choices = WidgetTheme.entries.map { Choice(it.id, it.id, stringResource(widgetThemeDescription(it))) },
        current = WidgetTheme.byId(config.appearance.widgetTheme).id,
        onSelect = { onChange("appearance", "widgetTheme", it) },
    )
    WhText(stringResource(R.string.style_widgets_hint), color = WhTheme.colors.darkForeground)
}

/** La descripción de cada temática en Estilo; una nueva sin la suya no compila. */
private fun widgetThemeDescription(theme: WidgetTheme): Int =
    when (theme) {
        WidgetTheme.Nova -> R.string.style_widget_theme_nova
        WidgetTheme.Hack -> R.string.style_widget_theme_hack
    }
