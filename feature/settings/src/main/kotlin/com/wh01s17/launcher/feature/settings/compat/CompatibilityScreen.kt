package com.wh01s17.launcher.feature.settings.compat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.components.WhToggle
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilitySnapshot
import com.wh01s17.launcher.feature.settings.R

/**
 * Ajustes → Compatibilidad (plan.md §5.9): estado de los gestos wh01s17, el modo que oculta los
 * botones, cómo evitar que HyperOS cierre el servicio, cómo dar el permiso por ADB y cómo salir
 * si el teléfono se queda sin navegación.
 */
@Composable
fun CompatibilityScreen(
    state: CompatibilityState,
    onModeChange: (Boolean) -> Unit,
    actions: CompatibilityActions,
    modifier: Modifier = Modifier,
) {
    val spacing = WhTheme.spacing
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(spacing.panelPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.panelPadding),
    ) {
        WhText(stringResource(R.string.compat_title), style = WhTheme.typography.display, color = WhTheme.colors.accent)
        WhText(stringResource(R.string.compat_intro), color = WhTheme.colors.darkForeground)
        val snapshot = state.snapshot ?: return@Column
        DeviceSection(snapshot)
        GesturesSection(state, snapshot, onModeChange, actions.openAccessibility)
        BackgroundSection(actions)
        AdbSection(snapshot.packageName, actions.openDeveloperOptions)
        RecoverySection()
    }
}

@Composable
private fun DeviceSection(snapshot: CompatibilitySnapshot) {
    val device = snapshot.device
    Section(stringResource(R.string.compat_section_device)) {
        InfoRow(stringResource(R.string.compat_model), "${device.brand} ${device.model}")
        InfoRow(
            stringResource(R.string.compat_system),
            stringResource(R.string.compat_system_value, device.xiaomiOsVersion ?: "Android", device.sdk),
        )
        if (!device.isXiaomi) Note(stringResource(R.string.compat_not_xiaomi))
    }
}

@Composable
private fun GesturesSection(
    state: CompatibilityState,
    snapshot: CompatibilitySnapshot,
    onModeChange: (Boolean) -> Unit,
    openAccessibility: () -> Unit,
) {
    val colors = WhTheme.colors
    Section(stringResource(R.string.compat_section_gestures)) {
        val (serviceText, serviceColor) =
            when {
                snapshot.serviceRunning -> stringResource(R.string.compat_service_running) to colors.accent
                state.serviceStuck -> stringResource(R.string.compat_service_stuck) to colors.yellow
                else -> stringResource(R.string.compat_service_off) to colors.darkForeground
            }
        InfoRow(stringResource(R.string.compat_service), serviceText, serviceColor)
        ActionButton(stringResource(R.string.compat_open_accessibility), openAccessibility)
        InfoRow(
            stringResource(R.string.compat_permission),
            stringResource(if (snapshot.permissionGranted) R.string.compat_permission_granted else R.string.compat_permission_missing),
            if (snapshot.permissionGranted) colors.accent else colors.yellow,
        )
        InfoRow(
            stringResource(R.string.compat_home),
            stringResource(if (snapshot.isDefaultHome) R.string.compat_yes else R.string.compat_no),
        )
        InfoRow(
            stringResource(R.string.compat_buttons),
            if (snapshot.forceFsgNavBar == 1) {
                stringResource(R.string.compat_buttons_hidden)
            } else {
                stringResource(R.string.compat_buttons_visible, snapshot.forceFsgNavBar?.toString() ?: "—")
            },
        )
        WhToggle(
            checked = snapshot.prefs.enabled,
            onCheckedChange = onModeChange,
            label = stringResource(R.string.compat_mode),
            enabled = state.modeToggleEnabled,
        )
        Note(stringResource(if (state.modeToggleEnabled) R.string.compat_mode_help else R.string.compat_mode_needs))
        statusText(snapshot)?.let { Note(it, colors.foreground) }
        snapshot.lastRestoreAtMillis?.let { Note(stringResource(R.string.compat_last_restore, formatTime(it)), colors.yellow) }
    }
}

@Composable
private fun BackgroundSection(actions: CompatibilityActions) {
    Section(stringResource(R.string.compat_section_background)) {
        Note(stringResource(R.string.compat_background_help), WhTheme.colors.foreground)
        ActionButton(stringResource(R.string.compat_open_app_details), actions.openAppDetails)
        ActionButton(stringResource(R.string.compat_open_autostart), actions.openAutostart)
    }
}

@Composable
private fun AdbSection(
    packageName: String,
    openDeveloperOptions: () -> Unit,
) {
    Section(stringResource(R.string.compat_section_adb)) {
        Note(stringResource(R.string.compat_adb_steps), WhTheme.colors.foreground)
        CommandRow("adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS")
        Note(stringResource(R.string.compat_adb_no_pc))
        ActionButton(stringResource(R.string.compat_open_developer), openDeveloperOptions)
    }
}

@Composable
private fun RecoverySection() {
    Section(stringResource(R.string.compat_section_recovery)) {
        Note(stringResource(R.string.compat_recovery_reboot), WhTheme.colors.foreground)
        CommandRow("adb shell settings put global force_fsg_nav_bar 0")
        Note(stringResource(R.string.compat_recovery_official), WhTheme.colors.foreground)
        CommandRow("adb shell cmd role add-role-holder android.app.role.HOME com.miui.home 0")
        CommandRow("adb shell settings put global force_fsg_nav_bar 1")
        Note(stringResource(R.string.compat_recovery_force_stop), WhTheme.colors.yellow)
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
) {
    WhButton(onClick = onClick) { WhText(text) }
}

/** Título de sección como en la galería y la barra: `-- título --`. */
@Composable
internal fun Section(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
        WhText("-- $title --", style = WhTheme.typography.caption, color = WhTheme.colors.darkForeground)
        content()
    }
}
