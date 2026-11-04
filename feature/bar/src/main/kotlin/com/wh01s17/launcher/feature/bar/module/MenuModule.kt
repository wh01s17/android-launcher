package com.wh01s17.launcher.feature.bar.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.feature.bar.R

/** `core.menu`: el botón del menú wh01s17, en verde como en la barra del escritorio. */
internal class MenuModule : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val style = rememberModuleStyle(context.settings, emptyList(), moduleDefaults(color = WhTheme.colors.accent))
        BarItem(
            style = style,
            contentDescription = stringResource(R.string.bar_menu),
            onClick = { context.onCommand(LauncherCommand.OpenMenu) },
        ) {
            WhIcon(WhIcons.Hyprland)
        }
    }

    companion object {
        const val ID = "core.menu"
    }
}
