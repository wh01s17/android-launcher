package com.wh01s17.launcher.feature.bar.module

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhIcon
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.icons.WhGlyph
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.status.BatteryState
import com.wh01s17.launcher.feature.bar.R

/**
 * `wh01s17.power`: batería. Clases: `charging`, `full`, `low` (≤ 20 %), `critical` (≤ 10 %) y
 * `power-save`. Por defecto `charging` va en verde, `low` en amarillo y `critical` en rojo; con
 * `classColors` se cambia. Sin batería (una tablet) no pinta nada.
 */
internal class PowerModule : BarModule {
    override val id = ID

    @Composable
    override fun Content(context: BarModuleContext) {
        val battery = context.state.battery
        val percent = battery.percent ?: return
        val colors = WhTheme.colors
        val classes = classesOf(battery)
        val style =
            rememberModuleStyle(
                context.settings,
                classes,
                moduleDefaults(classColors = mapOf(CHARGING to colors.accent, LOW to colors.yellow, CRITICAL to colors.red)),
            )
        val description =
            when {
                battery.full -> stringResource(R.string.bar_battery_full)
                battery.charging -> stringResource(R.string.bar_battery_charging, percent)
                else -> stringResource(R.string.bar_battery, percent)
            } + if (battery.powerSave) stringResource(R.string.bar_battery_power_save) else ""
        BarItem(style, contentDescription = description) {
            WhIcon(glyphOf(battery, percent))
            WhText("$percent%", maxLines = 1)
        }
    }

    companion object {
        const val ID = "wh01s17.power"
        const val CHARGING = "charging"
        const val FULL = "full"
        const val LOW = "low"
        const val CRITICAL = "critical"
        const val POWER_SAVE = "power-save"
        private const val HIGH_PERCENT = 80
        private const val MEDIUM_PERCENT = 40

        /** Las clases del módulo, de más a menos importante: el color lo decide la primera con color. */
        fun classesOf(battery: BatteryState): List<String> =
            buildList {
                if (battery.critical) add(CRITICAL)
                if (battery.low) add(LOW)
                if (battery.charging) add(CHARGING)
                if (battery.full) add(FULL)
                if (battery.powerSave) add(POWER_SAVE)
            }

        /** El glifo del shell es un rayo al cargar y una pila de tres niveles si no. */
        fun glyphOf(
            battery: BatteryState,
            percent: Int,
        ): WhGlyph =
            when {
                battery.charging -> WhIcons.Charging
                percent >= HIGH_PERCENT -> WhIcons.BatteryFull
                percent >= MEDIUM_PERCENT -> WhIcons.BatteryMedium
                else -> WhIcons.BatteryLow
            }
    }
}
