package com.wh01s17.launcher.feature.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.wh01s17.launcher.core.config.BarConfig
import com.wh01s17.launcher.core.config.ConfigParser
import com.wh01s17.launcher.core.config.ParseResult
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.status.AudioState
import com.wh01s17.launcher.core.system.status.BatteryState
import com.wh01s17.launcher.core.system.status.BluetoothStatus
import com.wh01s17.launcher.core.system.status.ClockGranularity
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.core.system.status.DoNotDisturbState
import com.wh01s17.launcher.core.system.status.NetworkKind
import com.wh01s17.launcher.core.system.status.NetworkState
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.PomodoroPreset
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import com.wh01s17.launcher.feature.bar.clock.CalendarPanel
import com.wh01s17.launcher.feature.bar.module.BarModuleRegistry
import com.wh01s17.launcher.feature.bar.timer.MetronomeIcon
import com.wh01s17.launcher.feature.bar.timer.MetronomeModule
import com.wh01s17.launcher.feature.bar.timer.PomodoroModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Capturas de referencia de la barra (plan.md §7 Hito 3), a 480 dpi y 407 dp de ancho: la pantalla
 * del Redmi Note 13 Pro 5G. La primera usa la config de fábrica tal cual, así que si deja de caber,
 * falla la comparación.
 *
 * Grabar:   ./gradlew :feature:bar:recordRoborazziDebug
 * Verificar: ./gradlew :feature:bar:verifyRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w407dp-h904dp-xxhdpi")
class BarScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Martes 3 de noviembre de 2026, 21:54:51, como la captura del escritorio. */
    private val fixedTime = ZonedDateTime.of(2026, 11, 3, 21, 54, 51, 0, ZoneId.of("UTC"))

    private val clock =
        object : ClockSource {
            override fun now() = fixedTime

            override fun ticks(granularity: ClockGranularity): Flow<ZonedDateTime> = flowOf(fixedTime)
        }

    @get:Rule
    val folder = TemporaryFolder()

    private val epoch = fixedTime.toInstant().toEpochMilli()

    /** Un pomodoro con la hora fija; su temporizador no avanza (nadie mueve el [TestScope]). */
    private fun pomodoro(setup: PomodoroRepository.() -> Unit = {}): PomodoroRepository =
        PomodoroRepository(folder.newFile(), TestScope()) { epoch }.apply(setup)

    private fun metronome() = MetronomeRepository(folder.newFile())

    private fun registry() = defaultBarRegistry(clock, pomodoro(), metronome())

    private fun config(json: String? = null): BarConfig {
        val text = json ?: File("../../core/config/src/main/assets/default-launcher.json").readText()
        return (ConfigParser.parse(text) as ParseResult.Success).config.bar
    }

    private val calm =
        BarState(
            battery = BatteryState(percent = 85, charging = false, full = false, powerSave = false),
            network = NetworkState(NetworkKind.Wifi, hasInternet = true, vpn = false),
            bluetooth = BluetoothStatus.Connected,
            audio = AudioState(volumePercent = 60, muted = false),
            workspaces = WorkspaceBarState(focused = 1, occupied = setOf(1)),
        )

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            WhTheme {
                Box(Modifier.fillMaxWidth().background(WhTheme.colors.background)) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun defaultBar() =
        capture("bar-default") {
            Bar(config(), registry(), calm, onCommand = {})
        }

    /**
     * Pomodoro en reposo (sólo el icono), trabajando, en pausa en el descanso corto y en el largo
     * del ultradiano (`h:mm:ss`); metrónomo parado y sonando.
     */
    @Test
    fun timers() =
        capture("bar-timers") {
            val timers =
                config(
                    """{"version":1,"bar":{"layout":{"left":[{"id":"pomodoro"}],"center":[],"right":[{"id":"wh01s17.metronome"}]}}}""",
                )
            val later = epoch + 5 * 60_000 + 9_000
            val states =
                listOf(
                    pomodoro(),
                    pomodoro { toggle() },
                    pomodoro {
                        toggle()
                        skip()
                        toggle()
                    },
                    pomodoro {
                        setPreset(PomodoroPreset.Ultradian)
                        toggle()
                        skip()
                        skip()
                        skip()
                    },
                )
            Column {
                states.forEach { repo ->
                    val modules = listOf(PomodoroModule(repo) { later }, MetronomeModule(metronome()))
                    Bar(timers, BarModuleRegistry(modules), calm, onCommand = {})
                }
                MetronomeIcon(running = true, ticks = 1, bpm = 100, modifier = Modifier.padding(WhTheme.spacing.controlGap))
            }
        }

    @Test
    fun everyState() =
        capture("bar-states") {
            val states =
                listOf(
                    calm.copy(workspaces = WorkspaceBarState(focused = 3, occupied = setOf(1, 3, 9))),
                    // El scratchpad con apps y a la vista: su «S» magenta, subrayada.
                    calm.copy(
                        workspaces =
                            WorkspaceBarState(
                                focused = 3,
                                occupied = setOf(1, 3),
                                scratchpadOccupied = true,
                                scratchpadVisible = true,
                            ),
                    ),
                    calm.copy(
                        battery = BatteryState(percent = 18, charging = false, full = false, powerSave = false),
                        network = NetworkState(NetworkKind.Wifi, hasInternet = false, vpn = false),
                        bluetooth = BluetoothStatus.Off,
                        audio = AudioState(volumePercent = 0, muted = true),
                        doNotDisturb = DoNotDisturbState(active = true),
                    ),
                    calm.copy(
                        battery = BatteryState(percent = 8, charging = false, full = false, powerSave = true),
                        network = NetworkState.Offline,
                        notices = listOf(BarNotice("config", "config", NoticeSeverity.Error)),
                    ),
                    calm.copy(
                        battery = BatteryState(percent = 64, charging = true, full = false, powerSave = false),
                        network = NetworkState(NetworkKind.Cellular, hasInternet = true, vpn = false),
                        audio = AudioState(volumePercent = 30, muted = false),
                        notices = listOf(BarNotice("nav", "gestos", NoticeSeverity.Warning)),
                    ),
                )
            val registry = registry()
            Column {
                // Todos los módulos de estado (la barra por defecto ya no lleva red, bluetooth ni batería).
                val all =
                    config(
                        """{"version":1,"bar":{"layout":{"left":[{"id":"core.menu"},{"id":"core.workspaces"}],
                        "center":[{"id":"wh01s17.clock","format":"HH:mm"}],
                        "right":[{"id":"wh01s17.indicators"},{"id":"wh01s17.network"},{"id":"wh01s17.bluetooth"},
                        {"id":"wh01s17.audio"},{"id":"wh01s17.power"}]}}}""",
                    )
                states.forEach { Bar(all, registry, it, onCommand = {}) }
                // Opciones de módulo: marco `[ ]`, color por clase y un módulo desconocido.
                Bar(
                    config(
                        """{"version":1,"bar":{"layout":{"left":[{"id":"core.menu"},{"id":"git-branch"}],
                        "center":[{"id":"wh01s17.clock","format":"HH:mm","brackets":true}],
                        "right":[{"id":"wh01s17.power","brackets":true,"classColors":{"default":"#45d9ea"}}]}}}""",
                    ),
                    registry,
                    calm,
                    onCommand = {},
                )
                // Con fondo propio y contorno.
                Bar(
                    config(
                        """{"version":1,"bar":{"transparent":false,"outline":true,
                        "layout":{"left":[{"id":"core.menu"},{"id":"core.workspaces"}]}}}""",
                    ),
                    registry,
                    calm,
                    onCommand = {
                    },
                )
            }
        }

    @Test
    fun calendar() =
        capture("bar-calendar") {
            Box(Modifier.padding(WhTheme.spacing.gapsOut)) {
                CalendarPanel(LocalDate.of(2026, 11, 3), DayOfWeek.MONDAY, Locale.forLanguageTag("es"))
            }
        }
}
