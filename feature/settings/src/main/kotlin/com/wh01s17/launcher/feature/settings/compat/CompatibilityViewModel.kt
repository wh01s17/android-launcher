package com.wh01s17.launcher.feature.settings.compat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilityGateway
import com.wh01s17.launcher.core.system.oem.xiaomi.CompatibilitySnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class CompatibilityState(
    /** `null` hasta la primera lectura. */
    val snapshot: CompatibilitySnapshot? = null,
) {
    /** El servicio no está conectado aunque el usuario lo activó (murió y no se recuperó aún). */
    val serviceStuck: Boolean get() = snapshot?.let { it.serviceEnabled && !it.serviceRunning } == true

    /** Activar el modo sólo tiene sentido con la capa 3 viva y el permiso; desactivarlo, siempre. */
    val modeToggleEnabled: Boolean
        get() = snapshot?.let { it.prefs.enabled || (it.serviceRunning && it.permissionGranted) } == true
}

/**
 * Asistente de compatibilidad (plan.md §5.9): estado de los gestos wh01s17, el modo que oculta los
 * botones y los accesos a los ajustes del sistema. Se relee al volver a la pantalla y cada vez que
 * cambian los ajustes de navegación.
 */
class CompatibilityViewModel(
    private val gateway: CompatibilityGateway,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CompatibilityState())
    val state: StateFlow<CompatibilityState> = mutableState.asStateFlow()

    private val observer = gateway.observe(::refresh)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val snapshot = withContext(ioDispatcher) { gateway.snapshot() }
            mutableState.value = CompatibilityState(snapshot)
        }
    }

    fun setModeEnabled(enabled: Boolean) {
        if (enabled && !mutableState.value.modeToggleEnabled) return
        gateway.setModeEnabled(enabled)
        refresh()
    }

    override fun onCleared() {
        observer.close()
    }
}
