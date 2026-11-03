package com.wh01s17.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.menu.drawer.AppDrawer
import com.wh01s17.launcher.feature.menu.drawer.AppDrawerViewModel
import com.wh01s17.launcher.feature.menu.drawer.DrawerIntent
import android.graphics.Color as AndroidColor

/**
 * Pantalla de inicio (rol HOME). Por ahora: fondo wh01s17 y el drawer provisional de apps
 * (plan.md §7, Hito 2); los workspaces llegan en el Hito 4.
 */
class MainActivity : ComponentActivity() {
    private val container get() = (application as LauncherApplication).container

    private val drawer: AppDrawerViewModel by viewModels {
        viewModelFactory { initializer { AppDrawerViewModel(container.appRepository, container.appActions) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // El tema wh01s17 es siempre oscuro: iconos claros en las barras del
        // sistema aunque el teléfono esté en modo claro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
        // Un launcher no se cierra con "atrás" (el drawer registra el suyo encima).
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )
        setContent {
            WhTheme {
                val state by drawer.state.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize()) {
                    HomeScreen(
                        onOpenApps = { drawer.onIntent(DrawerIntent.Open) },
                        onOpenSettings = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
                    )
                    AppDrawer(state, container.iconLoader, drawer::onIntent)
                }
            }
        }
    }

    /**
     * Vigilancia de plan.md §5.9: si los botones están ocultos y la navegación propia no está viva,
     * se devuelven y se avisa. Como launcher HOME, esto corre en cada vuelta al inicio y al arrancar.
     */
    override fun onResume() {
        super.onResume()
        if (container.navigationWatchdog.check()) {
            Toast.makeText(this, R.string.navigation_buttons_restored, Toast.LENGTH_LONG).show()
        }
    }

    /** Pulsar Inicio estando ya en el launcher vuelve a la pantalla limpia. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            drawer.onIntent(DrawerIntent.Close)
        }
    }
}
