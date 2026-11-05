package com.wh01s17.launcher.feature.settings.style

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.config.LauncherConfig
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhNumberField
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.components.WhToggle
import com.wh01s17.launcher.core.designsystem.layout.chromeAndKeyboard
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.settings.R

@Composable
fun StyleScreen(
    config: LauncherConfig,
    error: String?,
    wallpapersOnly: Boolean,
    onChange: (String, String, Any) -> Unit,
    actions: StyleActions,
    modifier: Modifier = Modifier,
    wallpaperPreview: @Composable () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.chromeAndKeyboard)
            .verticalScroll(rememberScrollState())
            .padding(WhTheme.spacing.panelPadding),
        verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
    ) {
        Heading(if (wallpapersOnly) R.string.style_wallpapers else R.string.style_title)
        if (error != null) WhText(error, color = WhTheme.colors.red)
        if (wallpapersOnly) {
            wallpaperPreview()
            Wallpapers(config, onChange, actions)
        } else {
            Appearance(config, onChange, actions)
            Screensaver(config, onChange, actions)
            Heading(R.string.style_config)
            Action(R.string.style_import_config, actions.importConfig)
            Action(R.string.style_export_config, actions.exportConfig)
            WhText(stringResource(R.string.style_config_hint), color = WhTheme.colors.darkForeground)
        }
    }
}

@Composable
private fun Appearance(
    config: LauncherConfig,
    onChange: (String, String, Any) -> Unit,
    actions: StyleActions,
) {
    WhText(stringResource(R.string.style_theme), color = WhTheme.colors.accent)
    WhText(stringResource(R.string.style_theme_hint), color = WhTheme.colors.darkForeground)
    NumberSetting(R.string.style_font, (config.appearance.fontScale * PERCENT).toInt(), FONT_RANGE) {
        onChange("appearance", "fontScale", it.toFloat() / PERCENT)
    }
    WhToggle(config.bar.transparent, { onChange("bar", "transparent", it) }, label = stringResource(R.string.style_transparent))
    NumberSetting(R.string.style_gaps_in, config.general.gapsIn ?: DEFAULT_GAP, GAP_RANGE) { onChange("general", "gapsIn", it) }
    NumberSetting(R.string.style_gaps_out, config.general.gapsOut ?: DEFAULT_GAP, GAP_RANGE) { onChange("general", "gapsOut", it) }
    NumberSetting(R.string.style_rounding, config.general.rounding ?: DEFAULT_RADIUS, GAP_RANGE) { onChange("general", "rounding", it) }
    Heading(R.string.style_icons)
    ICONS.forEach { (value, label) ->
        WhButton({ onChange("apps", "iconStyle", value) }, selected = config.apps.iconStyle == value) { WhText(stringResource(label)) }
    }
    Action(R.string.style_wallpapers, actions.wallpapers)
}

@Composable
private fun Wallpapers(
    config: LauncherConfig,
    onChange: (String, String, Any) -> Unit,
    actions: StyleActions,
) {
    WALLPAPERS.forEach { (value, label) ->
        WhButton(
            { onChange("appearance", "wallpaper", value) },
            selected = config.appearance.wallpaper == value,
        ) { WhText(stringResource(label)) }
    }
    Action(R.string.style_import_image, actions.importImage)
    Action(R.string.style_system_wallpaper, actions.systemWallpaper)
    WhText(stringResource(R.string.style_wallpaper_hint), color = WhTheme.colors.darkForeground)
}

@Composable
private fun Screensaver(
    config: LauncherConfig,
    onChange: (String, String, Any) -> Unit,
    actions: StyleActions,
) {
    Heading(R.string.style_screensaver)
    NumberSetting(R.string.style_idle, config.idle.screensaver, IDLE_RANGE) { onChange("idle", "screensaver", it) }
    WhText(stringResource(R.string.style_idle_hint), color = WhTheme.colors.darkForeground)
    Action(R.string.style_preview, actions.preview)
    Action(R.string.style_dream_settings, actions.dreamSettings)
}

@Composable
private fun NumberSetting(
    label: Int,
    value: Int,
    range: IntRange,
    onValue: (Int) -> Unit,
) {
    Column {
        WhText(stringResource(label))
        Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap), verticalAlignment = Alignment.CenterVertically) {
            WhButton({ onValue((value - 1).coerceIn(range)) }, enabled = value > range.first) { WhText("−") }
            WhNumberField(
                value,
                { onValue(it.coerceIn(range)) },
                style = WhTheme.typography.title,
                maxDigits = 4,
                editLabel = stringResource(label),
            )
            WhButton({ onValue((value + 1).coerceIn(range)) }, enabled = value < range.last) { WhText("+") }
        }
    }
}

@Composable
private fun Heading(label: Int) = WhText(stringResource(label), style = WhTheme.typography.title, color = WhTheme.colors.accent)

@Composable
private fun Action(
    label: Int,
    action: () -> Unit,
) = WhButton(action) { WhText(stringResource(label)) }

private const val PERCENT = 100
private const val DEFAULT_GAP = 2
private const val DEFAULT_RADIUS = 10
private val FONT_RANGE = 75..200
private val GAP_RANGE = 0..32
private val IDLE_RANGE = 0..3600
private val ICONS =
    listOf(
        "monochrome" to R.string.style_monochrome,
        "original" to R.string.style_original,
        "pixelated" to R.string.style_pixelated,
    )
private val WALLPAPERS =
    listOf(
        "none" to R.string.style_none,
        "mark" to R.string.style_mark,
        "moon" to R.string.style_moon,
        "moon-wordmark" to R.string.style_moon_wordmark,
        "imported" to R.string.style_imported,
    )
