package com.wh01s17.launcher

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.feature.bar.Bar
import com.wh01s17.launcher.feature.bar.BarViewModel
import com.wh01s17.launcher.feature.bar.barHeight
import com.wh01s17.launcher.feature.menu.panel.LauncherMenu
import com.wh01s17.launcher.feature.menu.panel.MenuViewModel
import com.wh01s17.launcher.feature.menu.search.MenuSearchViewModel
import com.wh01s17.launcher.feature.menu.search.SearchIntent
import com.wh01s17.launcher.feature.workspaces.WorkspacesIntent
import com.wh01s17.launcher.feature.workspaces.WorkspacesViewModel
import com.wh01s17.launcher.feature.workspaces.ui.WorkspacesScreen
import com.wh01s17.launcher.input.Shortcuts
import com.wh01s17.launcher.ui.HomeReturn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

/**
 * Pantalla de inicio (rol HOME): los escritorios en mosaico (Hito 4), la barra encima (Hito 3) y el
 * menú y paleta unificados (Hito 5), también usados para seleccionar apps para tiles.
 */
class MainActivity : ComponentActivity() {
    private val container get() = (application as LauncherApplication).container

    private val search: MenuSearchViewModel by viewModels {
        viewModelFactory {
            initializer {
                MenuSearchViewModel(container.appRepository, container.appActions, container.usageRepository, container.menuCommands)
            }
        }
    }

    private val menu: MenuViewModel by viewModels()
    private val homeReturn = HomeReturn()
    private val appVersion by lazy { packageManager.getPackageInfo(packageName, 0).versionName.orEmpty() }

    private val menuController: MenuController by lazy {
        MenuController(this, menu, search, container.notificationShade, container.commandBus::send, commands::handle)
    }

    private val commands: HomeCommands by lazy { HomeCommands(this, container, { menuController }, workspaces::onCommand) }

    /** Un atajo de escritorio con el menú abierto lo cierra antes, para ver lo que hace. */
    private val shortcuts by lazy {
        Shortcuts(container.configRepository.state, onSuperSeen = { container.superKeyDelivered = true }) { command ->
            if (command is WorkspaceCommand && menu.page.value != null) menuController.close()
            container.commandBus.send(command)
        }
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
        setContent { HomeContent() }
    }

    @Composable
    private fun HomeContent() {
        WhTheme {
            val state by search.state.collectAsStateWithLifecycle()
            val menuPage by menu.page.collectAsStateWithLifecycle()
            val barState by bar.state.collectAsStateWithLifecycle()
            val workspacesState by workspaces.state.collectAsStateWithLifecycle()
            val config by container.configRepository.state.collectAsStateWithLifecycle()
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        WhTheme.colors.background,
                    ).then(homeReturn.modifier(config.config?.animations?.workspaces)),
            ) {
                // Los escritorios ocupan lo que dejan la barra y los insets (la exclusive zone de
                // la barra en Hyprland): ningún tile queda debajo.
                WorkspacesScreen(
                    state = workspacesState,
                    iconLoader = container.iconLoader,
                    onIntent = workspaces::onIntent,
                    onCommand = workspaces::onCommand,
                    onAddTile = menuController::pick,
                    onOpenMenu = menuController::open,
                    onGesture = shortcuts::onGesture,
                    boundGestures = remember(config.config?.bindings) { Shortcuts.boundGestures(config.config) },
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
                LauncherMenu(
                    page = menuPage,
                    onNavigate = {
                        search.onIntent(SearchIntent.ClearQuery)
                        menu.navigate(it)
                    },
                    onBack = menuController::back,
                    onClose = menuController::close,
                    onAction = menuController::execute,
                    search = state,
                    iconLoader = container.iconLoader,
                    onSearch = search::onIntent,
                    version = appVersion,
                )
            }
        }
    }

    /**
     * Los comandos de la barra, el teclado, los gestos y el menú llegan por el bus;
     * la app elegida en el drawer en modo selección se convierte en un tile.
     */
    private fun handleCommands() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { search.picks.collect { workspaces.addApp(it.key) } }
                launch { search.closed.collect { menu.close() } }
                launch { search.commands.collect(menuController::execute) }
                container.commandBus.commands.collect(commands::handle)
            }
        }
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

    /** Se vuelve al inicio desde otra app: animación de vuelta (HyperOS no la hace con un launcher ajeno). */
    override fun onRestart() {
        super.onRestart()
        homeReturn.play()
    }

    /** Al salir del inicio se guardan ya los escritorios, por si Android cierra el proceso. */
    override fun onStop() {
        super.onStop()
        container.processScope.launch { container.desk.flush() }
    }

    /**
     * Los atajos de `bindings` van antes que la UI (con el menú abierto, sólo los que llevan `SUPER`,
     * `CTRL` o `ALT`, para no robar letras a la búsqueda). Escape también funciona en modo táctil,
     * cuando Compose no conserva un nodo con foco. AndroidX core marca su override como interno;
     * este es el callback público de Activity.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (shortcuts.onKeyEvent(event, typing = menu.page.value != null)) return true
        if (event.keyCode != KeyEvent.KEYCODE_ESCAPE) return super.dispatchKeyEvent(event)
        if (menu.page.value == null) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) menuController.back()
        return true
    }

    /** Pulsar Inicio estando ya en el launcher vuelve a la pantalla limpia. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            menuController.close()
            workspaces.onIntent(WorkspacesIntent.ExitEdit)
        }
    }
}
