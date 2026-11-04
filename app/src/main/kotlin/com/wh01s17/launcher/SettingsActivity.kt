package com.wh01s17.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import com.wh01s17.launcher.feature.settings.shortcuts.ShortcutTable
import com.wh01s17.launcher.feature.settings.shortcuts.ShortcutsScreen
import android.graphics.Color as AndroidColor

/**
 * Ajustes del launcher: Compatibilidad (plan.md §7, Hito 2), que se abre desde la barra de edición
 * o el menú, y Atajos (`Super + K`, Hito 6), con [shortcuts].
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
        if (intent.getStringExtra(EXTRA_PAGE) == PAGE_SHORTCUTS) {
            setContent { ShortcutsPage() }
            return
        }
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

    @Composable
    private fun ShortcutsPage() {
        WhTheme {
            val config by container.configRepository.state.collectAsStateWithLifecycle()
            val table = config.config?.let { ShortcutTable.of(it, container.superKeyDelivered) }
            ShortcutsScreen(table = table, modifier = Modifier.background(WhTheme.colors.background))
        }
    }

    /** Al volver de Ajustes del sistema (accesibilidad, batería…) el estado puede haber cambiado. */
    override fun onResume() {
        super.onResume()
        compatibility.refresh()
    }

    companion object {
        private const val EXTRA_PAGE = "page"
        private const val PAGE_SHORTCUTS = "shortcuts"

        /** La tabla de atajos (`Super + K`, plan.md §6.4). */
        fun shortcuts(context: Context): Intent = Intent(context, SettingsActivity::class.java).putExtra(EXTRA_PAGE, PAGE_SHORTCUTS)
    }
}
