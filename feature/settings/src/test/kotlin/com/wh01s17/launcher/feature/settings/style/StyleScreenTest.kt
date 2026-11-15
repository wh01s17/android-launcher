package com.wh01s17.launcher.feature.settings.style

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.wh01s17.launcher.core.config.AppearanceConfig
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
    private var previews = 0
    private val actions get() = StyleActions({}, { imports++ }, {}, {}, {}, { previews++ }, {})

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
        // La paleta y los iconos se eligen en su panel: la fila dice la actual y lo abre.
        compose.onNodeWithText("Paleta: phosphor").performScrollTo().performClick()
        compose.onNodeWithText("nord").performScrollTo().performClick()
        assertEquals(Triple("appearance", "palette", "nord"), changed.last())
        compose.onNodeWithText("Iconos: monocromos").performScrollTo().performClick()
        compose.onNodeWithText("originales").performClick()
        assertEquals(Triple("apps", "iconStyle", "original"), changed.last())
        compose.onNodeWithText("Barra transparente").performScrollTo().performClick()
        assertEquals(Triple("bar", "transparent", false), changed.last())
        compose.onNodeWithText("Tema").performScrollTo()
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

    @Test
    fun `the font scale is rounded so plus moves past 104`() {
        compose.setContent {
            WhTheme {
                StyleScreen(
                    LauncherConfig(1, BarConfig(), appearance = AppearanceConfig(fontScale = 1.05f)),
                    null,
                    false,
                    { section, key, value -> changed += Triple(section, key, value) },
                    actions,
                )
            }
        }
        // 1.05f × 100 = 104,99…: truncado se veía 104 y «+» volvía a guardar 1.05.
        compose.onNodeWithText("105").assertExists()
        compose.onAllNodesWithText("+")[0].performClick()
        assertEquals(Triple("appearance", "fontScale", 1.06f), changed.last())
    }

    @Test
    fun `the widget theme row opens a list and picking one saves it`() {
        compose.setContent {
            WhTheme {
                StyleScreen(
                    LauncherConfig(1, BarConfig()),
                    null,
                    false,
                    { section, key, value -> changed += Triple(section, key, value) },
                    actions,
                )
            }
        }
        compose.onNodeWithText("Temática: nova").performScrollTo().performClick()
        // El panel lista todas las temáticas; elegir una la guarda y lo cierra.
        compose.onNodeWithText("Temática de los widgets").assertExists()
        compose.onNodeWithText("hack").performClick()
        assertEquals(Triple("appearance", "widgetTheme", "hack"), changed.last())
        compose.onNodeWithText("Temática de los widgets").assertDoesNotExist()
    }

    @Test
    fun `the screensaver options show a live preview that opens full screen`() {
        compose.setContent {
            WhTheme {
                StyleScreen(LauncherConfig(1, BarConfig()), null, false, { _, _, _ -> }, actions)
            }
        }
        // La miniatura no tiene texto: se encuentra por la acción de toque, que abre la vista previa.
        val miniature =
            SemanticsMatcher("opens the full screen preview") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "Vista previa"
            }
        compose.onNode(miniature).performScrollTo().performClick()
        assertEquals(1, previews)
    }
}
