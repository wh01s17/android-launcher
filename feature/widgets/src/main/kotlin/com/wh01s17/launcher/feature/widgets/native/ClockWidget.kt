package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.status.ClockGranularity
import com.wh01s17.launcher.core.system.status.ClockSource
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * Reloj y calendario (plan.md §7 Hito 7): hora grande en acento, la fecha y la semana de lunes a
 * domingo con el día de hoy subrayado, como el popup del reloj de la barra.
 */
@Composable
internal fun ClockWidget(
    clock: ClockSource,
    modifier: Modifier = Modifier,
) {
    val now by remember(clock) { clock.ticks(ClockGranularity.Minute) }.collectAsStateWithLifecycle(clock.now())
    val colors = WhTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier.fillMaxSize().background(colors.background).padding(WhTheme.spacing.controlGap),
        verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WhText(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = WhTheme.typography.displayLarge,
            color = colors.accent,
            maxLines = 1,
        )
        WhText(now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale)), color = colors.foreground, maxLines = 1)
        val monday = now.toLocalDate().with(DayOfWeek.MONDAY)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (offset in 0L until DAYS_PER_WEEK) {
                val day = monday.plusDays(offset)
                val today = day == now.toLocalDate()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WhText(
                        day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        style = WhTheme.typography.caption,
                        color = colors.darkForeground,
                    )
                    WhText("${day.dayOfMonth}", color = if (today) colors.accent else colors.foreground)
                    Box(
                        Modifier
                            .height(WhTheme.spacing.barUnderlineHeight)
                            .fillMaxWidth(UNDERLINE_FRACTION)
                            .background(if (today) colors.accent else colors.background),
                    )
                }
            }
        }
    }
}

private const val DAYS_PER_WEEK = 7L
private const val UNDERLINE_FRACTION = 0.6f
