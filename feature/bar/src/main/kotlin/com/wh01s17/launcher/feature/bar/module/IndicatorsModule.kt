package com.wh01s17.launcher.feature.bar.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.feature.bar.R

/**
 * `wh01s17.indicators`: avisos de estado que sólo ocupan sitio mientras están activos. Hoy, No
 * molestar (clase `dnd`); el contador de notificaciones llega con el acceso a notificaciones
 * (Hito 8, opt-in). Sin nada activo no pinta nada.
 */
internal class IndicatorsModule : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        if (!context.state.doNotDisturb.active) return
        val style = rememberModuleStyle(context.settings, listOf(DND), moduleDefaults())
        BarItem(style, contentDescription = stringResource(R.string.bar_dnd)) { WhIcon(WhIcons.Moon) }
    }

    companion object {
        const val ID = "wh01s17.indicators"
        const val DND = "dnd"
    }
}
