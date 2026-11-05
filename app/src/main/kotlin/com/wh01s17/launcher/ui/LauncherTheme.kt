package com.wh01s17.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.apps.icons.IconStyle
import com.wh01s17.launcher.core.apps.icons.LocalIconStyle
import com.wh01s17.launcher.core.config.AppearanceConfig
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.di.AppContainer

@Composable
internal fun LauncherTheme(
    container: AppContainer,
    content: @Composable () -> Unit,
) {
    val state by container.configRepository.state.collectAsStateWithLifecycle()
    val config = state.config
    CompositionLocalProvider(LocalIconStyle provides IconStyle.fromConfig(config?.apps?.iconStyle)) {
        WhTheme(fontScale = config?.appearance?.fontScale ?: AppearanceConfig.DEFAULT_FONT_SCALE, content = content)
    }
}
