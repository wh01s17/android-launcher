package com.wh01s17.launcher.feature.settings.style

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.wh01s17.launcher.core.config.BarConfig
import com.wh01s17.launcher.core.config.LauncherConfig
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
@Config(sdk = [35], qualifiers = "w407dp-h904dp-xxhdpi")
class StyleScreenTest {
    @get:Rule val compose = createComposeRule()
    private val changed = mutableListOf<Triple<String, String, Any>>()
    private var imports = 0
    private val actions get() = StyleActions({}, { imports++ }, {}, {}, {}, {}, {})

    @Test
    fun appearance() {
        compose.setContent {
            WhTheme {
                StyleScreen(
                    LauncherConfig(1, BarConfig()),
                    null,
                    false,
                    { section, key, value -> changed += Triple(section, key, value) },
                    actions,
                    Modifier.background(WhTheme.colors.background),
                )
            }
        }
        compose.onNodeWithText("Originales").performClick()
        assertEquals(Triple("apps", "iconStyle", "original"), changed.last())
        compose.onNodeWithText("Barra transparente").performClick()
        assertEquals(Triple("bar", "transparent", false), changed.last())
        compose.onRoot().captureRoboImage("src/test/screenshots/style.png")
    }

    @Test
    fun wallpapers() {
        compose.setContent {
            WhTheme {
                StyleScreen(
                    LauncherConfig(1, BarConfig()),
                    null,
                    true,
                    { section, key, value -> changed += Triple(section, key, value) },
                    actions,
                    Modifier.background(WhTheme.colors.background),
                )
            }
        }
        compose.onNodeWithText("Luna llena").performClick()
        assertEquals(Triple("appearance", "wallpaper", "moon"), changed.last())
        compose.onNodeWithText("Importar imagen…").performClick()
        assertEquals(1, imports)
        compose.onRoot().captureRoboImage("src/test/screenshots/wallpapers.png")
    }
}
