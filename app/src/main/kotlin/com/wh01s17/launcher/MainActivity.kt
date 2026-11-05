package com.wh01s17.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wh01s17.launcher.core.designsystem.layout.chrome
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.system.notifications.BadgeKey
import com.wh01s17.launcher.core.system.notifications.NotificationState
import com.wh01s17.launcher.feature.bar.Bar
import com.wh01s17.launcher.feature.bar.BarViewModel
import com.wh01s17.launcher.feature.bar.barHeight
import com.wh01s17.launcher.feature.menu.panel.LauncherMenu
import com.wh01s17.launcher.feature.menu.panel.MenuViewModel
import com.wh01s17.launcher.feature.menu.search.MenuSearchViewModel
import com.wh01s17.launcher.feature.menu.search.SearchIntent
import com.wh01s17.launcher.feature.workspaces.WorkspacesIntent
import com.wh01s17.launcher.feature.workspaces.WorkspacesViewModel
import com.wh01s17.launcher.feature.workspaces.ui.LocalTileBadges
import com.wh01s17.launcher.feature.workspaces.ui.LocalWidgetRenderer
import com.wh01s17.launcher.feature.workspaces.ui.TileBadges
import com.wh01s17.launcher.feature.workspaces.ui.WorkspacesScreen
import com.wh01s17.launcher.input.Shortcuts
import com.wh01s17.launcher.ui.HomeNotifications
import com.wh01s17.launcher.ui.HomeReturn
import com.wh01s17.launcher.ui.HomeWallpaper
import com.wh01s17.launcher.ui.IdleOverlay
import com.wh01s17.launcher.ui.IdleScreensaver
import com.wh01s17.launcher.ui.LauncherTheme
import com.wh01s17.launcher.ui.NotificationOptIn
import com.wh01s17.launcher.ui.screenReaderSemantics
import com.wh01s17.launcher.widgets.HomeWidgets
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
    private val idle by lazy { IdleScreensaver(this, container.configRepository) }
    private var wakingGesture = false
    private lateinit var widgets: HomeWidgets

    /** El permiso de notificaciones, tras explicarlo ([NotificationOptIn]). */
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { container.timers.notificationsAnswered(declined = false) }
    private val appVersion by lazy { packageManager.getPackageInfo(packageName, 0).versionName.orEmpty() }

    private val menuController: MenuController by lazy {
        MenuController(
            this,
            menu,
            search,
            container.notificationShade,
            container.commandBus::send,
            commands::handle,
        ) { widgets.openPicker() }
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
        idle.start()
        // Registra el resultado del diálogo de enlace de widgets: tiene que ser antes de onStart.
        widgets =
            HomeWidgets(
                this,
                container.widgetHost,
                container.desk.state,
                container.nativeWidgets,
                onAudioPermission = { container.audioPermission.value = it },
                setRequestAudio = { container.requestAudioPermission = it },
                onAdded = workspaces::addWidget,
                onAddNative = workspaces::addNative,
            )
        setContent { HomeContent() }
    }

    @Composable
    private fun HomeContent() {
        LauncherTheme(container) {
            val state by search.state.collectAsStateWithLifecycle()
            val menuPage by menu.page.collectAsStateWithLifecycle()
            val barState by bar.state.collectAsStateWithLifecycle()
            val workspacesState by workspaces.state.collectAsStateWithLifecycle()
            val config by container.configRepository.state.collectAsStateWithLifecycle()
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        WhTheme.colors.oledBackground,
                    ).then(homeReturn.modifier(config.config?.animations?.workspaces))
                    .then(screenReaderSemantics()),
            ) {
                // Los escritorios ocupan lo que dejan la barra y los insets (la exclusive zone de
                // la barra en Hyprland): ningún tile queda debajo. El teclado no cuenta: los tapa sin
                // cambiar su tamaño (ver `WhInsets`).
                HomeWallpaper(config.config?.appearance?.wallpaper, container.wallpapers)
                val notificationState by container.notifications.state.collectAsStateWithLifecycle(NotificationState())
                val tileBadges = remember(notificationState.badges) { badgesOf(notificationState.badges) }
                CompositionLocalProvider(LocalWidgetRenderer provides widgets.renderer, LocalTileBadges provides tileBadges) {
                    WorkspacesScreen(
                        state = workspacesState,
                        iconLoader = container.iconLoader,
                        onIntent = workspaces::onIntent,
                        onCommand = workspaces::onCommand,
                        onAddTile = menuController::pick,
                        onAddWidget = widgets::openPicker,
                        onOpenMenu = menuController::open,
                        onGesture = shortcuts::onGesture,
                        boundGestures = remember(config.config?.bindings) { Shortcuts.boundGestures(config.config) },
                        onOpenSettings = { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
                        modifier =
                            Modifier
                                .windowInsetsPadding(WindowInsets.chrome.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(top = barHeight()),
                    )
                }
                // La barra va debajo de la barra de estado del sistema (plan.md §5.5). Hasta que
                // se lee la config no hay barra que pintar.
                config.config?.let { launcherConfig ->
                    Bar(
                        config = launcherConfig.bar,
                        registry = container.barRegistry,
                        state = barState,
                        onCommand = bar::send,
                        // Debajo de la barra de estado de Android (plan.md §5.5).
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                    )
                }
                HomeNotifications(container, {
                    openNotificationAccess(this@MainActivity, container.notifications)
                }, notificationState, config.config?.notifications)
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
                widgets.Picker()
                val askNotifications by container.timers.askNotifications.collectAsStateWithLifecycle()
                if (askNotifications) {
                    NotificationOptIn(
                        onAllow = ::requestNotifications,
                        onDecline = { container.timers.notificationsAnswered(declined = true) },
                    )
                }
                IdleOverlay(idle)
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

    /** Android 13+ tiene diálogo de permiso; antes, las notificaciones se activan en los ajustes de la app. */
    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            container.timers.notificationsAnswered(declined = false)
        }
    }

    /**
     * Vigilancia de plan.md §5.9: si los botones están ocultos y la navegación propia no está viva,
     * se devuelven y se avisa. Como launcher HOME, esto corre en cada vuelta al inicio y al arrancar.
     */
    override fun onResume() {
        super.onResume()
        container.timers.ensureService()
        container.notifications.ensureBound()
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
        if (idle.onKey(event)) return true
        if (shortcuts.onKeyEvent(event, typing = menu.page.value != null)) return true
        if (event.keyCode != KeyEvent.KEYCODE_ESCAPE) return super.dispatchKeyEvent(event)
        if (container.notificationPanelOpen.value) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) container.notificationPanelOpen.value = false
            return true
        }
        if (widgets.pickerOpen) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) widgets.closePicker()
            return true
        }
        if (menu.page.value == null) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) menuController.back()
        return true
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            wakingGesture = idle.visible.value
            idle.touch()
        }
        if (wakingGesture) {
            if (event.action == android.view.MotionEvent.ACTION_UP ||
                event.action == android.view.MotionEvent.ACTION_CANCEL
            ) {
                wakingGesture = false
            }
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    /** La configuración de un widget nuevo vuelve aquí (`AppWidgetHost` usa la API antigua). */
    @Deprecated("AppWidgetHost.startAppWidgetConfigureActivityForResult sólo entrega aquí")
    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        if (!widgets.onConfigureResult(requestCode, resultCode)) super.onActivityResult(requestCode, resultCode, data)
    }

    /** Pulsar Inicio estando ya en el launcher vuelve a la pantalla limpia. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            menuController.close()
            container.notificationPanelOpen.value = false
            workspaces.onIntent(WorkspacesIntent.ExitEdit)
        }
    }
}

/** Lo que un tile pregunta: cuántas notificaciones tiene su app (y su perfil). */
private fun badgesOf(badges: Map<BadgeKey, Int>) = TileBadges { app -> badges[BadgeKey(app.packageName, app.userSerial)] ?: 0 }

private fun openNotificationAccess(
    activity: ComponentActivity,
    repository: com.wh01s17.launcher.core.system.notifications.NotificationRepository,
) {
    try {
        activity.startActivity(repository.accessSettings())
    } catch (ignored: ActivityNotFoundException) {
        Toast.makeText(activity, R.string.notification_access_unavailable, Toast.LENGTH_LONG).show()
    }
}
