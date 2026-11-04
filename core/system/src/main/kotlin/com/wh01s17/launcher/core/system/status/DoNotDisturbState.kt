package com.wh01s17.launcher.core.system.status

import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.flow.Flow

/** No molestar activo (módulo `wh01s17.indicators`). Leerlo no exige permisos. */
data class DoNotDisturbState(
    val active: Boolean,
) {
    companion object {
        /** `INTERRUPTION_FILTER_ALL` y `UNKNOWN` son "sin filtro"; prioridad, nada y alarmas, filtro activo. */
        fun ofFilter(filter: Int): DoNotDisturbState =
            DoNotDisturbState(
                filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                    filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                    filter == NotificationManager.INTERRUPTION_FILTER_ALARMS,
            )
    }
}

class DoNotDisturbSource(
    private val context: Context,
) : StatusSource<DoNotDisturbState> {
    private val notifications = context.getSystemService(NotificationManager::class.java)

    override fun observe(): Flow<DoNotDisturbState> =
        context.broadcastState(intentFilterOf(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)) {
            DoNotDisturbState.ofFilter(notifications.currentInterruptionFilter)
        }
}
