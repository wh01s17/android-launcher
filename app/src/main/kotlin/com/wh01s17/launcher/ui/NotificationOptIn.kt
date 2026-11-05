package com.wh01s17.launcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wh01s17.launcher.R
import com.wh01s17.launcher.core.designsystem.components.WhButton
import com.wh01s17.launcher.core.designsystem.components.WhPanel
import com.wh01s17.launcher.core.designsystem.components.WhScrim
import com.wh01s17.launcher.core.designsystem.components.WhText
import com.wh01s17.launcher.core.designsystem.layout.chrome
import com.wh01s17.launcher.core.designsystem.theme.WhTheme

/**
 * Antes de pedir el permiso de notificaciones (plan.md R8), qué se gana con él: ver y parar el
 * pomodoro y el metrónomo desde las notificaciones, y el aviso al terminar cada fase. Sin él todo
 * sigue funcionando. Sale la primera vez que algo corre sin el permiso.
 */
@Composable
fun NotificationOptIn(
    onAllow: () -> Unit,
    onDecline: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WhScrim(onClick = onDecline, clickLabel = stringResource(R.string.timer_optin_later))
        WhPanel(
            Modifier
                .windowInsetsPadding(WindowInsets.chrome)
                .padding(WhTheme.spacing.panelPadding)
                .widthIn(max = WhTheme.spacing.menuWidth),
        ) {
            WhText(stringResource(R.string.timer_optin_title), style = WhTheme.typography.title, color = WhTheme.colors.accent)
            WhText(stringResource(R.string.timer_optin_body))
            Row(
                Modifier.padding(top = WhTheme.spacing.panelGap),
                horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
            ) {
                WhButton(onClick = onAllow, selected = true) { WhText(stringResource(R.string.timer_optin_allow)) }
                WhButton(onClick = onDecline) { WhText(stringResource(R.string.timer_optin_later)) }
            }
        }
    }
}
