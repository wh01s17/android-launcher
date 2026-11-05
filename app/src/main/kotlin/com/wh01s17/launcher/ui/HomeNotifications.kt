package com.wh01s17.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wh01s17.launcher.core.config.NotificationsConfig
import com.wh01s17.launcher.core.system.notifications.NotificationItem
import com.wh01s17.launcher.core.system.notifications.NotificationState
import com.wh01s17.launcher.di.AppContainer
import com.wh01s17.launcher.feature.bar.barHeight
import com.wh01s17.launcher.feature.notifications.NotificationPanel
import com.wh01s17.launcher.feature.notifications.NotificationToastHost

/** Panel de notificaciones (bajo la barra) y, si se pide en la config, el aviso de las nuevas. */
@Composable
internal fun HomeNotifications(
    container: AppContainer,
    onGrantAccess: () -> Unit,
    state: NotificationState,
    settings: NotificationsConfig?,
) {
    val open by container.notificationPanelOpen.collectAsStateWithLifecycle()
    val catalog by container.appRepository.catalog.collectAsStateWithLifecycle()
    val labels = remember(catalog) { catalog.apps.associate { it.packageName to it.label } }
    val labelOf = { item: NotificationItem -> labels[item.packageName] ?: item.packageName }
    val belowBar = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = barHeight())
    if (open) {
        BackHandler { container.notificationPanelOpen.value = false }
        NotificationPanel(
            state = state,
            labelOf = labelOf,
            now = remember { System.currentTimeMillis() },
            onOpen = { item ->
                container.notifications.open(item)
                container.notificationPanelOpen.value = false
            },
            onDismiss = container.notifications::dismiss,
            onClearAll = container.notifications::dismissAll,
            onGrantAccess = {
                container.notificationPanelOpen.value = false
                onGrantAccess()
            },
            onClose = { container.notificationPanelOpen.value = false },
            modifier = belowBar,
        )
    }
    NotificationToastHost(
        items = state.items,
        enabled = settings?.toast == true && !open,
        seconds = settings?.toastSeconds ?: NotificationsConfig.DEFAULT_TOAST_SECONDS,
        labelOf = labelOf,
        onOpen = container.notifications::open,
        modifier = belowBar,
    )
}
