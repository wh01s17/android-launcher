package com.wh01s17.launcher.feature.bar.clock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.feature.bar.R
import com.wh01s17.launcher.feature.bar.module.BarItem
import com.wh01s17.launcher.feature.bar.module.BarModule
import com.wh01s17.launcher.feature.bar.module.BarModuleContext
import com.wh01s17.launcher.feature.bar.module.moduleDefaults
import com.wh01s17.launcher.feature.bar.module.rememberModuleStyle

/**
 * `wh01s17.clock`: hora y fecha con los formatos de `shell.json` (`format`, `formatAlt`). Tocar
 * abre el calendario semanal (`weekStartDay`, lunes por defecto) y mantener pulsado alterna entre
 * `format` y `formatAlt`, como el clic derecho del escritorio. Sólo tiene temporizador mientras la
 * barra se ve, y de segundos únicamente si el formato muestra segundos.
 *
 * Ojo: el formato alterno no se guarda en la config; vuelve a `format` al reiniciar el launcher.
 */
internal class ClockModule(
    private val clock: ClockSource,
) : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val settings = context.settings
        var alternate by rememberSaveable { mutableStateOf(false) }
        var calendarOpen by rememberSaveable { mutableStateOf(false) }
        val pattern = if (alternate) settings.string(FORMAT_ALT) ?: DEFAULT_ALT else settings.string(FORMAT) ?: DEFAULT_FORMAT
        val granularity = ClockFormat.granularity(pattern)
        val ticks = remember(clock, granularity) { clock.ticks(granularity) }
        val now by ticks.collectAsStateWithLifecycle(initialValue = clock.now())
        val locale = LocalConfiguration.current.locales[0]
        val style = rememberModuleStyle(settings, emptyList(), moduleDefaults())
        BarItem(
            style = style,
            contentDescription = stringResource(R.string.bar_clock_calendar),
            onClick = { calendarOpen = !calendarOpen },
            onLongClick = { alternate = !alternate },
        ) {
            WhText(ClockFormat.format(now, pattern, locale), maxLines = 1)
        }
        if (calendarOpen) {
            CalendarPopup(
                today = now.toLocalDate(),
                weekStart = CalendarModel.parseWeekStart(settings.string(WEEK_START)),
                locale = locale,
                onDismiss = { calendarOpen = false },
            )
        }
    }

    companion object {
        const val ID = "wh01s17.clock"
        const val FORMAT = "format"
        const val FORMAT_ALT = "formatAlt"
        const val WEEK_START = "weekStartDay"

        /** Los de `BarWidget.qml` del reloj del shell. */
        const val DEFAULT_FORMAT = "dddd HH:mm"
        const val DEFAULT_ALT = "d MMMM 'W'ww yyyy"
    }
}
