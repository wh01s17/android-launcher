package com.wh01s17.launcher.feature.menu.panel

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import com.wh01s17.launcher.core.apps.AppRepository
import com.wh01s17.launcher.core.apps.actions.AppActions
import com.wh01s17.launcher.core.apps.actions.AppShortcut
import com.wh01s17.launcher.core.apps.actions.LaunchOrigin
import com.wh01s17.launcher.core.apps.actions.LaunchResult
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.apps.icons.IconRequest
import com.wh01s17.launcher.core.apps.model.AppCatalog
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.apps.model.AppProfile
import com.wh01s17.launcher.core.apps.model.ProfileKind
import com.wh01s17.launcher.core.designsystem.icons.WhIcons
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.menu.search.MenuSearchViewModel
import com.wh01s17.launcher.feature.menu.search.SearchIntent
import com.wh01s17.launcher.feature.menu.search.SearchTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w407dp-h904dp-xxhdpi")
class SearchKeyboardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app = AppEntry(AppKey("calculator", "calculator.Main", 0), "Calculadora")
    private val repository =
        object : AppRepository {
            override val catalog = MutableStateFlow(AppCatalog.of(listOf(AppProfile(0, ProfileKind.Personal)), listOf(app)))
        }
    private val actions =
        object : AppActions {
            override fun launch(
                app: AppEntry,
                origin: LaunchOrigin?,
            ) = LaunchResult.Launched

            override fun openAppInfo(
                app: AppEntry,
                origin: LaunchOrigin?,
            ) = LaunchResult.Launched

            override fun requestUninstall(app: AppEntry) = LaunchResult.Launched

            override fun canReadShortcuts() = false

            override fun shortcuts(
                app: AppEntry,
                limit: Int,
            ) = emptyList<AppShortcut>()

            override fun launchShortcut(
                shortcut: AppShortcut,
                origin: LaunchOrigin?,
            ) = LaunchResult.Launched
        }
    private val icons =
        object : AppIconLoader {
            override fun cached(request: IconRequest): android.graphics.Bitmap? = null

            override suspend fun load(request: IconRequest): android.graphics.Bitmap? = null
        }
    private val vm =
        MenuSearchViewModel(
            repository,
            actions,
            commands =
                listOf(
                    SearchTarget.Command(MenuAction.Wifi, "Ajustes Wi-Fi", WhIcons.Wifi),
                    SearchTarget.Command(MenuAction.Bluetooth, "Ajustes Bluetooth", WhIcons.Bluetooth),
                ),
            workDispatcher = Dispatchers.Unconfined,
            ioDispatcher = Dispatchers.Unconfined,
        )
    private val sent = mutableListOf<MenuAction>()

    private fun content() {
        composeRule.setContent {
            val state by vm.state.collectAsState()
            LaunchedEffect(Unit) {
                vm.onIntent(SearchIntent.Open)
                vm.commands.collect { sent += it }
            }
            WhTheme {
                LauncherMenu(
                    MenuPage.Root,
                    {},
                    {},
                    {},
                    {},
                    search = state,
                    iconLoader = icons,
                    onSearch = vm::onIntent,
                )
            }
        }
    }

    @Test
    fun typingTabShiftTabArrowsAndEnterOperateOnTheUnifiedResults() {
        content()
        composeRule.onNode(hasSetTextAction()).performTextInput("ajustes")
        composeRule.onNodeWithText("Ajustes Bluetooth").assertExists()
        composeRule.onRoot().performKeyInput {
            keyDown(Key.Tab)
            keyUp(Key.Tab)
        }
        composeRule.runOnIdle { assertEquals(1, vm.state.value.selected) }
        composeRule.onRoot().performKeyInput {
            keyDown(Key.ShiftLeft)
            keyDown(Key.Tab)
            keyUp(Key.Tab)
            keyUp(Key.ShiftLeft)
        }
        composeRule.runOnIdle { assertEquals(0, vm.state.value.selected) }
        composeRule.onRoot().performKeyInput {
            keyDown(Key.DirectionDown)
            keyUp(Key.DirectionDown)
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/menu-search.png")
        composeRule.onRoot().performKeyInput {
            keyDown(Key.Enter)
            keyUp(Key.Enter)
        }
        composeRule.runOnIdle { assertEquals(listOf(MenuAction.Bluetooth), sent) }
    }

    @Test
    fun shiftF10OpensAppContextMenuAndEscapeDismissesIt() {
        content()
        composeRule.onNode(hasSetTextAction()).performTextInput("Calculadora")
        composeRule.onRoot().performKeyInput {
            keyDown(Key.ShiftLeft)
            keyDown(Key.F10)
            keyUp(Key.F10)
            keyUp(Key.ShiftLeft)
        }
        composeRule.runOnIdle { assertNotNull(vm.state.value.menu) }
        composeRule.onNode(isDialog()).performKeyInput {
            keyDown(Key.Escape)
            keyUp(Key.Escape)
        }
        composeRule.runOnIdle { assertNull(vm.state.value.menu) }
    }
}
