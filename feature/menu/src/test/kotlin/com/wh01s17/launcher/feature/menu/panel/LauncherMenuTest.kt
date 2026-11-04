package com.wh01s17.launcher.feature.menu.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w407dp-h904dp-xxhdpi", manifest = Config.NONE)
class LauncherMenuTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rootAndSystemNavigation() {
        var page by mutableStateOf<MenuPage?>(MenuPage.Root)
        var action: MenuAction? = null
        composeRule.setContent {
            WhTheme {
                Box(Modifier.fillMaxSize().background(WhTheme.colors.background)) {
                    LauncherMenu(page, { page = it }, { page = MenuPage.Root }, { page = null }, { action = it })
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/menu-root.png")
        composeRule.onNodeWithText("Sistema").performScrollTo().performClick()
        composeRule.onNodeWithText("Wi-Fi").performClick()
        assertEquals(MenuAction.Wifi, action)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/menu-system.png")
        composeRule.onRoot().performKeyInput { keyDown(Key.Escape) }
        assertEquals(MenuPage.System, page)
        composeRule.onRoot().performKeyInput { keyUp(Key.Escape) }
        assertEquals(MenuPage.Root, page)
        composeRule.onNodeWithText("Apps").performScrollTo().performClick()
        assertEquals(MenuAction.Apps, action)
    }

    @Test
    @Config(qualifiers = "w904dp-h407dp-xxhdpi")
    fun shortWindowKeepsLastEntryReachable() {
        var page by mutableStateOf<MenuPage?>(MenuPage.Root)
        composeRule.setContent {
            WhTheme { LauncherMenu(page, { page = it }, {}, {}, {}) }
        }
        composeRule.onNodeWithText("Sistema").performScrollTo().performClick()
        assertEquals(MenuPage.System, page)
        composeRule.onNodeWithText("Ajustes de Android").performScrollTo().assertExists()
    }
}
