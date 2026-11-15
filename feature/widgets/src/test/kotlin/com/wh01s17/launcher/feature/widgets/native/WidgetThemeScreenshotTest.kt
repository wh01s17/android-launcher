package com.wh01s17.launcher.feature.widgets.native

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.designsystem.widgets.LocalWidgetTheme
import com.wh01s17.launcher.core.designsystem.widgets.WidgetTheme
import com.wh01s17.launcher.core.system.media.AudioSpectrum
import com.wh01s17.launcher.core.system.media.MediaSource
import com.wh01s17.launcher.core.system.status.ClockGranularity
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Los widgets propios con cada temática (Estilo → Widgets), a 480 dpi como el Redmi: un tile mediano
 * y uno grande de cada uno. Sirven de referencia al añadir temáticas. El de uso del sistema se revisa
 * en el teléfono (sus datos son los de la máquina que corre la prueba).
 *
 * Grabar:   ./gradlew :feature:widgets:recordRoborazziDebug
 * Verificar: ./gradlew :feature:widgets:verifyRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w407dp-h1400dp-xxhdpi")
class WidgetThemeScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    /** Martes 3 de noviembre de 2026, 21:54:51, como las capturas de la barra. */
    private val fixedTime = ZonedDateTime.of(2026, 11, 3, 21, 54, 51, 0, ZoneId.of("UTC"))

    private val clock =
        object : ClockSource {
            override fun now() = fixedTime

            override fun ticks(granularity: ClockGranularity) = flowOf(fixedTime)
        }

    private fun sources(): NativeWidgetSources {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val epoch = fixedTime.toInstant().toEpochMilli()
        return NativeWidgetSources(
            media = MediaSource(context),
            pomodoro = PomodoroRepository(folder.newFile(), TestScope()) { epoch },
            metronome = MetronomeRepository(folder.newFile()),
            clock = clock,
            spectrum = AudioSpectrum(context),
            audioPermission = MutableStateFlow(false),
            requestAudio = {},
        )
    }

    private fun capture(
        theme: WidgetTheme,
        ids: List<String>,
    ) {
        val sources = sources()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalWidgetTheme provides theme) {
                WhTheme {
                    Column(
                        Modifier.background(WhTheme.colors.oledBackground).padding(WhTheme.spacing.controlGap),
                        verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
                    ) {
                        ids.forEach { id -> Pair(id, sources).Row() }
                    }
                }
            }
        }
        // Las lecturas del sistema van en segundo plano (Dispatchers.IO): se les deja terminar.
        repeat(SETTLE_ROUNDS) {
            Thread.sleep(SETTLE_SLEEP_MILLIS)
            composeRule.mainClock.advanceTimeByFrame()
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/widgets-${theme.id}-${ids.first()}.png")
    }

    /** Mediano (190 dp) y grande (380 dp de alto), lado a lado. */
    @Composable
    private fun Pair<String, NativeWidgetSources>.Row() {
        Row(horizontalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap)) {
            NativeWidgetView(first, second, interactive = true, modifier = Modifier.size(190.dp, 190.dp))
            NativeWidgetView(first, second, interactive = true, modifier = Modifier.size(190.dp, 380.dp))
        }
    }

    @Test
    fun hackClockAndTimers() = capture(WidgetTheme.Hack, listOf("clock", "pomodoro", "metronome"))

    // Sin «usage»: en Robolectric lee el /sys del PC (frecuencias, tiempo encendido) y la captura cambiaría
    // en cada ejecución.
    @Test
    fun hackMedia() = capture(WidgetTheme.Hack, listOf("tidal"))

    /** El reloj hack a pantalla completa: `tty-clock`, avances, `cal` y `fortune`. */
    @Test
    fun hackClockFull() {
        val sources = sources()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalWidgetTheme provides WidgetTheme.Hack) {
                WhTheme { NativeWidgetView("clock", sources, interactive = true, modifier = Modifier.size(390.dp, 800.dp)) }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/widgets-hack-clock-full.png")
    }

    /** El reloj ancho del escritorio 1 del usuario (todo el ancho, ~190 dp de alto) en las dos temáticas. */
    @Test
    fun wideClocks() {
        val sources = sources()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            WhTheme {
                Column(
                    Modifier.background(WhTheme.colors.oledBackground).padding(WhTheme.spacing.controlGap),
                    verticalArrangement = Arrangement.spacedBy(WhTheme.spacing.controlGap),
                ) {
                    WidgetTheme.entries.forEach { theme ->
                        CompositionLocalProvider(LocalWidgetTheme provides theme) {
                            NativeWidgetView("clock", sources, interactive = true, modifier = Modifier.size(390.dp, 190.dp))
                        }
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/widgets-clock-wide.png")
    }

    @Test
    fun novaClockAndTimers() = capture(WidgetTheme.Nova, listOf("clock", "pomodoro", "metronome"))

    private companion object {
        const val SETTLE_ROUNDS = 10
        const val SETTLE_SLEEP_MILLIS = 50L
    }
}
