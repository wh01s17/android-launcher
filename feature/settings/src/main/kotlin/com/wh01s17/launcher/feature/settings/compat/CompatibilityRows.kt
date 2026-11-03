package com.wh01s17.launcher.feature.settings.compat

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilitySnapshot
import com.wh01s17.launcher.core.system.oem.xiaomi.GestureModeStatus
import com.wh01s17.launcher.feature.settings.R
import java.text.DateFormat
import java.util.Date

/** Una fila `etiqueta …… valor`. */
@Composable
internal fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = WhTheme.colors.foreground,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WhText(label, Modifier.weight(1f), color = WhTheme.colors.darkForeground)
        WhText(value, color = valueColor)
    }
}

/** Texto explicativo, más pequeño. */
@Composable
internal fun Note(
    text: String,
    color: Color = WhTheme.colors.darkForeground,
) {
    WhText(text, style = WhTheme.typography.bodySmall, color = color)
}

/** Un comando para copiar al portapapeles y pegarlo en la terminal del PC. */
@Composable
internal fun CommandRow(command: String) {
    val context = LocalContext.current
    val copied = stringResource(R.string.compat_copied)
    val spacing = WhTheme.spacing
    Row(
        Modifier
            .fillMaxWidth()
            .background(WhTheme.colors.lighterBackground, WhTheme.borders.shape)
            .padding(horizontal = spacing.controlPaddingX, vertical = spacing.controlPaddingY),
        horizontalArrangement = Arrangement.spacedBy(spacing.controlGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WhText(command, Modifier.weight(1f), style = WhTheme.typography.caption, color = WhTheme.colors.brightForeground)
        WhButton(
            onClick = {
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("adb", command))
                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
            },
        ) { WhText(stringResource(R.string.compat_copy), style = WhTheme.typography.caption) }
    }
}

/**
 * Qué hizo la capa 2 en su última decisión, con la hora. `null` si nunca decidió nada o si el
 * servicio no está activo: entonces esa decisión ya no describe el teléfono.
 */
@Composable
internal fun statusText(snapshot: CompatibilitySnapshot): String? {
    val status = snapshot.status?.takeIf { snapshot.serviceRunning } ?: return null
    val text =
        stringResource(
            when (status.status) {
                GestureModeStatus.Off -> R.string.compat_status_off
                GestureModeStatus.NotHome -> R.string.compat_status_not_home
                GestureModeStatus.MissingPermission -> R.string.compat_status_permission
                GestureModeStatus.ButtonsHidden -> R.string.compat_status_hidden
                GestureModeStatus.GaveUp -> R.string.compat_status_gave_up
                GestureModeStatus.Restored -> R.string.compat_status_restored
            },
        )
    return stringResource(R.string.compat_status_at, text, formatTime(status.atMillis))
}

internal fun formatTime(millis: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
