package com.wh01s17.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.oem.xiaomi.SystemScreens
import com.wh01s17.launcher.feature.settings.compat.CompatibilityActions
import com.wh01s17.launcher.feature.settings.compat.CompatibilityScreen
import com.wh01s17.launcher.feature.settings.compat.CompatibilityViewModel
import android.graphics.Color as AndroidColor

/**
 * Ajustes del launcher. Por ahora sólo Compatibilidad (plan.md §7, Hito 2); se abre manteniendo
 * pulsado el inicio. El resto de ajustes llega con el menú del Hito 5.
 */
class SettingsActivity : ComponentActivity() {
    private val container get() = (application as LauncherApplication).container

    private val compatibility: CompatibilityViewModel by viewModels {
        viewModelFactory { initializer { CompatibilityViewModel(container.compatibilityGateway) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        val actions =
            CompatibilityActions(
                openAccessibility = { SystemScreens.accessibility(this) },
                openAppDetails = { SystemScreens.appDetails(this) },
                openAutostart = { SystemScreens.autostart(this) },
                openDeveloperOptions = { SystemScreens.developerOptions(this) },
            )
        setContent {
            WhTheme {
                val state by compatibility.state.collectAsStateWithLifecycle()
                CompatibilityScreen(
                    state = state,
                    onModeChange = compatibility::setModeEnabled,
                    actions = actions,
                    modifier = Modifier.fillMaxSize().background(WhTheme.colors.background),
                )
            }
        }
    }

    /** Al volver de Ajustes del sistema (accesibilidad, batería…) el estado puede haber cambiado. */
    override fun onResume() {
        super.onResume()
        compatibility.refresh()
    }
}
