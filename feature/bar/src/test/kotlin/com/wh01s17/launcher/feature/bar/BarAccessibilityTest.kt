package com.wh01s17.launcher.feature.bar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.wh01s17.launcher.core.config.ConfigParser
import com.wh01s17.launcher.core.config.ParseResult
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.media.MediaSource
import com.wh01s17.launcher.core.system.status.AudioState
import com.wh01s17.launcher.core.system.status.BatteryState
import com.wh01s17.launcher.core.system.status.BluetoothStatus
import com.wh01s17.launcher.core.system.status.ClockGranularity
import com.wh01s17.launcher.core.system.status.ClockSource
import com.wh01s17.launcher.core.system.status.DoNotDisturbState
import com.wh01s17.launcher.core.system.status.NetworkKind
import com.wh01s17.launcher.core.system.status.NetworkState
import com.wh01s17.launcher.core.system.timer.MetronomeRepository
import com.wh01s17.launcher.core.system.timer.PomodoroRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * TalkBack y áreas táctiles de la barra (plan.md §7 Hito 11): todo lo que se puede tocar tiene algo
 * que leer y un área de al menos 48 dp, aunque el módulo se dibuje más pequeño (Compose amplía el área
 * hasta `minimumTouchTargetSize`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w407dp-h904dp-xxhdpi")
class BarAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val now = ZonedDateTime.of(2026, 11, 3, 21, 54, 51, 0, ZoneId.of("UTC"))

    private val clock =
        object : ClockSource {
            override fun now() = now

            override fun ticks(granularity: ClockGranularity) = flowOf(now)
        }

    private val everyModule =
        """{"version":1,"bar":{"layout":{"left":[{"id":"core.menu"},{"id":"core.workspaces"},{"id":"pomodoro"}],
        "center":[{"id":"wh01s17.clock","format":"HH:mm"}],
        "right":[{"id":"wh01s17.metronome"},{"id":"wh01s17.indicators"},{"id":"wh01s17.network"},
        {"id":"wh01s17.bluetooth"},{"id":"wh01s17.audio"},{"id":"wh01s17.power"}]}}}"""

    private val state =
        BarState(
            battery = BatteryState(percent = 85, charging = false, full = false, powerSave = false),
            network = NetworkState(NetworkKind.Wifi, hasInternet = true, vpn = false),
            bluetooth = BluetoothStatus.Connected,
            audio = AudioState(volumePercent = 60, muted = false),
            doNotDisturb = DoNotDisturbState(active = true),
            workspaces = WorkspaceBarState(focused = 2, occupied = setOf(1, 2, 3)),
            notices = listOf(BarNotice("config", "config", NoticeSeverity.Error)),
        )

    @Test
    fun `every touchable bar item has a label and a 48 dp target`() {
        val epoch = now.toInstant().toEpochMilli()
        val registry =
            defaultBarRegistry(
                clock,
                PomodoroRepository(folder.newFile(), TestScope()) { epoch },
                MetronomeRepository(folder.newFile()),
                MediaSource(ApplicationProvider.getApplicationContext()),
            )
        val config = (ConfigParser.parse(everyModule) as ParseResult.Success).config.bar
        composeRule.setContent {
            WhTheme {
                Box(Modifier.fillMaxWidth().background(WhTheme.colors.background)) {
                    Bar(config, registry, state, onCommand = {})
                }
            }
        }
        val touchable =
            composeRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick), useUnmergedTree = false)
                .fetchSemanticsNodes()
        // Menú, 3 escritorios, pomodoro, reloj, metrónomo, avisos y los cinco de estado.
        assertTrue("found only ${touchable.size} touchable items", touchable.size >= 10)
        val density = composeRule.density
        val minimum = with(density) { 48.dp.toPx() } - 1f
        val problems =
            touchable.mapNotNull { node ->
                val label = node.label()
                val bounds = node.touchBoundsInRoot
                when {
                    label.isBlank() -> "unlabeled node at ${node.boundsInRoot}"
                    bounds.width < minimum || bounds.height < minimum -> "'$label' touch target ${bounds.size} < 48 dp"
                    else -> null
                }
            }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun SemanticsNode.label(): String {
        val config = config
        val description = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        val text = config.getOrNull(SemanticsProperties.Text).orEmpty()
        return (description + text.map { it.text }).joinToString(" ")
    }
}
