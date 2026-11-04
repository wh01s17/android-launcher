package com.wh01s17.launcher.bar

import android.content.Context
import com.wh01s17.launcher.R
import com.wh01s17.launcher.core.config.ConfigError
import com.wh01s17.launcher.core.config.ConfigState
import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilityGateway
import com.wh01s17.launcher.core.system.oem.xiaomi.GestureModeStatus
import com.wh01s17.launcher.feature.bar.BarNotice
import com.wh01s17.launcher.feature.bar.NoticeSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Los avisos de la barra (plan.md §5.9 y §6.2): una config que no se pudo aplicar (rojo) y la capa 2
 * de Xiaomi cuando HyperOS la deshizo demasiadas veces y se rindió (amarillo).
 */
class BarNotices(
    private val context: Context,
    private val config: Flow<ConfigState>,
    private val compatibility: CompatibilityGateway,
) {
    val notices: Flow<List<BarNotice>> =
        combine(config.map { it.errors }.distinctUntilChanged(), navigationGaveUp()) { errors, gaveUp ->
            listOfNotNull(configNotice(errors), if (gaveUp) navigationNotice() else null)
        }.distinctUntilChanged()

    private fun configNotice(errors: List<ConfigError>): BarNotice? =
        errors.takeIf { it.isNotEmpty() }?.let {
            BarNotice(CONFIG, context.getString(R.string.notice_config_invalid, it.joinToString("; ")), NoticeSeverity.Error)
        }

    private fun navigationNotice() = BarNotice(NAVIGATION, context.getString(R.string.notice_navigation_gave_up), NoticeSeverity.Warning)

    /** `true` mientras el servicio de gestos corre y la capa 2 se rindió. Lee ajustes: fuera del hilo principal. */
    private fun navigationGaveUp(): Flow<Boolean> =
        callbackFlow {
            val observer = compatibility.observe { trySend(Unit) }
            trySend(Unit)
            awaitClose { observer.close() }
        }.map {
            val snapshot = compatibility.snapshot()
            snapshot.serviceRunning && snapshot.status?.status == GestureModeStatus.GaveUp
        }.distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    private companion object {
        const val CONFIG = "config"
        const val NAVIGATION = "navigation"
    }
}
