package com.wh01s17.launcher.feature.settings.compat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.components.WhToggle
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.navigation.EdgeSensitivity
import com.wh01s17.launcher.core.system.navigation.EdgeSettings
import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilitySnapshot
import com.wh01s17.launcher.feature.settings.R

/**
 * Gestos de borde (capa 3, plan.md §7 Hito 6): cada gesto por separado, grosor de las franjas y
 * sensibilidad. Se aplican al momento en `:nav`. Con los botones ocultos, *inicio* no se apaga.
 */
@Composable
internal fun EdgeSection(
    snapshot: CompatibilitySnapshot,
    onChange: ((EdgeSettings) -> EdgeSettings) -> Unit,
) {
    val edges = snapshot.edges
    val locked = snapshot.prefs.enabled
    Section(stringResource(R.string.compat_section_edges)) {
        WhToggle(edges.backLeft, { on -> onChange { it.copy(backLeft = on) } }, label = stringResource(R.string.compat_edge_back_left))
        WhToggle(edges.backRight, { on -> onChange { it.copy(backRight = on) } }, label = stringResource(R.string.compat_edge_back_right))
        WhToggle(
            edges.home || locked,
            { on -> onChange { it.copy(home = on) } },
            label = stringResource(R.string.compat_edge_home),
            enabled = !locked,
        )
        if (locked) Note(stringResource(R.string.compat_edge_home_locked))
        WhToggle(edges.recents, { on -> onChange { it.copy(recents = on) } }, label = stringResource(R.string.compat_edge_recents))
        ThicknessRow(stringResource(R.string.compat_edge_side), edges.sideThicknessDp) { dp -> onChange { it.copy(sideThicknessDp = dp) } }
        ThicknessRow(
            stringResource(R.string.compat_edge_bottom),
            edges.bottomThicknessDp,
        ) { dp -> onChange { it.copy(bottomThicknessDp = dp) } }
        SensitivityRow(edges.sensitivity) { level -> onChange { it.copy(sensitivity = level) } }
        Note(stringResource(R.string.compat_edge_keyboard))
    }
}

@Composable
private fun ThicknessRow(
    label: String,
    dp: Int,
    onChange: (Int) -> Unit,
) {
    val range = EdgeSettings.THICKNESS_DP
    val step = EdgeSettings.THICKNESS_STEP_DP
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WhText(label, Modifier.weight(1f), color = WhTheme.colors.darkForeground)
        WhButton(onClick = { onChange((dp - step).coerceIn(range)) }, enabled = dp > range.first) { WhText("−") }
        WhText(stringResource(R.string.compat_edge_dp, dp))
        WhButton(onClick = { onChange((dp + step).coerceIn(range)) }, enabled = dp < range.last) { WhText("+") }
    }
}

@Composable
private fun SensitivityRow(
    current: EdgeSensitivity,
    onChange: (EdgeSensitivity) -> Unit,
) {
    WhText(stringResource(R.string.compat_edge_sensitivity), color = WhTheme.colors.darkForeground)
    Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
        for (level in EdgeSensitivity.entries) {
            val label =
                when (level) {
                    EdgeSensitivity.Low -> R.string.compat_edge_low
                    EdgeSensitivity.Normal -> R.string.compat_edge_normal
                    EdgeSensitivity.High -> R.string.compat_edge_high
                }
            WhButton(onClick = { onChange(level) }, selected = level == current) { WhText(stringResource(label)) }
        }
    }
}
