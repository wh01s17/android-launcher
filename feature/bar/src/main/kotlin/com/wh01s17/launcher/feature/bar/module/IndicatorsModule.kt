package com.wh01s17.launcher.feature.bar.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.feature.bar.R

/**
 * `wh01s17.indicators`: avisos de estado que sólo ocupan sitio mientras están activos: No molestar
 * (clase `dnd`) y el contador de notificaciones (clase `notifications`, Hito 8), que sólo existe si
 * el usuario dio el acceso a notificaciones. Tocarlo abre el panel de notificaciones. Sin nada
 * activo no pinta nada.
 */
internal class IndicatorsModule : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val dnd = context.state.doNotDisturb.active
        val count =
            context.state.notifications.count
                .takeIf { context.state.notifications.granted } ?: 0
        if (!dnd && count == 0) return
        val classes = listOfNotNull(NOTIFICATIONS.takeIf { count > 0 }, DND.takeIf { dnd })
        val style =
            rememberModuleStyle(context.settings, classes, moduleDefaults(classColors = mapOf(NOTIFICATIONS to WhTheme.colors.accent)))
        val description =
            listOfNotNull(
                pluralStringResource(R.plurals.bar_notifications, count, count).takeIf { count > 0 },
                stringResource(R.string.bar_dnd).takeIf { dnd },
            ).joinToString(", ")
        BarItem(
            style,
            contentDescription = description,
            onClick = if (count > 0) ({ context.onCommand(LauncherCommand.ToggleNotificationPanel) }) else null,
        ) {
            if (count > 0) {
                WhIcon(WhIcons.Bell)
                WhText(if (count > MAX_SHOWN) "$MAX_SHOWN+" else count.toString())
            }
            if (dnd) WhIcon(WhIcons.Moon)
        }
    }

    companion object {
        const val ID = "wh01s17.indicators"
        const val DND = "dnd"
        const val NOTIFICATIONS = "notifications"
        private const val MAX_SHOWN = 99
    }
}
