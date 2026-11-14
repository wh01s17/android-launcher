package com.wh01s17.launcher.ui

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.wh01s17.launcher.core.config.ConfigRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * El launcher va siempre en vertical salvo que en Estilo se active *girar con el sensor*
 * (`appearance.autoRotate`); entonces sigue al sensor respetando el bloqueo de rotación del sistema.
 */
internal fun ComponentActivity.followRotationSetting(repository: ConfigRepository) {
    lifecycleScope.launch {
        repository.state
            .map { it.config?.appearance?.autoRotate ?: false }
            .distinctUntilChanged()
            .collect { rotate ->
                requestedOrientation =
                    if (rotate) ActivityInfo.SCREEN_ORIENTATION_USER else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
    }
}
