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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.LauncherCommand
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.feature.bar.Bar
import com.wh01s17.launcher.feature.bar.BarViewModel
import com.wh01s17.launcher.feature.bar.barHeight
import com.wh01s17.launcher.feature.menu.drawer.AppDrawer
import com.wh01s17.launcher.feature.menu.drawer.AppDrawerViewModel
import com.wh01s17.launcher.feature.menu.drawer.DrawerIntent
import com.wh01s17.launcher.feature.workspaces.WorkspacesIntent
import com.wh01s17.launcher.feature.workspaces.WorkspacesViewModel
import com.wh01s17.launcher.feature.workspaces.ui.WorkspacesScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

/**
 * Pantalla de inicio (rol HOME): los escritorios en mosaico (Hito 4), la barra encima (Hito 3) y el
 * drawer provisional de apps (Hito 2), que también elige la app de un tile nuevo.
 */
class MainActivity : ComponentActivity() {
    private val container get() = (application as LauncherApplication).container

    private val drawer: AppDrawerViewModel by viewModels {
        viewModelFactory { initializer { AppDrawerViewModel(container.appRepository, container.appActions) } }
    }

    private val bar: BarViewModel by viewModels {
        viewModelFactory {
            initializer {
                BarViewModel(container.barSources, container.workspaceBarState, container.barNotices.notices, container.commandBus)
            }
        }
    }

    private val workspaces: WorkspacesViewModel by viewModels {
        viewModelFactory {
            initializer {
                WorkspacesViewModel(
                    desk = container.desk,
                    catalog = container.appRepository.catalog,
                    config = container.configRepository.state.map { it.config },
                    actions = container.appActions,
                )
            }
        }
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
        handleCommands()
        setContent {
            WhTheme {
                val state by drawer.state.collectAsStateWithLifecycle()
                val barState by bar.state.collectAsStateWithLifecycle()
                val workspacesState by workspaces.state.collectAsStateWithLifecycle()
                val config by container.configRepository.state.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize().background(WhTheme.colors.background)) {
                    // Los escritorios ocupan lo que dejan la barra y los insets (la exclusive zone de
                    // la barra en Hyprland): ningún tile queda debajo.
                    WorkspacesScreen(
                        state = workspacesState,
                        iconLoader = container.iconLoader,
                        onIntent = workspaces::onIntent,
                        onCommand = workspaces::onCommand,
                        onAddTile = { drawer.onIntent(DrawerIntent.OpenPicker) },
                        onOpenMenu = { drawer.onIntent(DrawerIntent.Open) },
                        onOpenSettings = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(top = barHeight()),
                    )
                    // La barra va debajo de la barra de estado del sistema (plan.md §5.5). Hasta que
                    // se lee la config no hay barra que pintar.
                    config.config?.let { launcherConfig ->
                        Bar(
                            config = launcherConfig.bar,
                            registry = container.barRegistry,
                            state = barState,
                            onCommand = bar::send,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                        )
                    }
                    AppDrawer(state, container.iconLoader, drawer::onIntent)
                }
            }
        }
    }

    /**
     * Los comandos de la barra (y, más adelante, del teclado, los gestos y el menú) llegan por el bus;
     * la app elegida en el drawer en modo selección se convierte en un tile.
     */
    private fun handleCommands() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { drawer.picks.collect { workspaces.addApp(it.key) } }
                container.commandBus.commands.collect { command ->
                    when (command) {
                        LauncherCommand.OpenMenu -> drawer.onIntent(DrawerIntent.Open)
                        LauncherCommand.OpenNotificationShade -> openNotificationShade()
                        is WorkspaceCommand -> workspaces.onCommand(command)
                    }
                }
            }
        }
    }

    /**
     * Sin el servicio de accesibilidad no se puede abrir la cortina: no se hace nada y se explica la
     * primera vez (plan.md §7, Hito 3), para no repetirlo en cada deslizamiento.
     */
    private fun openNotificationShade() {
        if (container.notificationShade.open()) return
        if (container.oneTimeHints.shouldShow(HINT_SHADE)) Toast.makeText(this, R.string.shade_needs_service, Toast.LENGTH_LONG).show()
    }

    /**
     * Vigilancia de plan.md §5.9: si los botones están ocultos y la navegación propia no está viva,
     * se devuelven y se avisa. Como launcher HOME, esto corre en cada vuelta al inicio y al arrancar.
     */
    override fun onResume() {
        super.onResume()
        // Una config editada a mano (o importada) se aplica al volver al inicio.
        lifecycleScope.launch { container.configRepository.reload() }
        if (container.navigationWatchdog.check()) {
            Toast.makeText(this, R.string.navigation_buttons_restored, Toast.LENGTH_LONG).show()
        }
    }

    /** Al salir del inicio se guardan ya los escritorios, por si Android cierra el proceso. */
    override fun onStop() {
        super.onStop()
        container.processScope.launch { container.desk.flush() }
    }

    /** Pulsar Inicio estando ya en el launcher vuelve a la pantalla limpia. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            drawer.onIntent(DrawerIntent.Close)
            workspaces.onIntent(WorkspacesIntent.ExitEdit)
        }
    }

    private companion object {
        const val HINT_SHADE = "notification-shade-needs-service"
    }
}
