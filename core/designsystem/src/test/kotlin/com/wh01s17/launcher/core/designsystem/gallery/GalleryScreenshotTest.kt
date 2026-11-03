package com.wh01s17.launcher.core.designsystem.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas de referencia de la galería (plan.md §7 Hito 1), una por sección, a 480 dpi y
 * 407 dp de ancho: la pantalla del Redmi Note 13 Pro 5G.
 *
 * Grabar:   ./gradlew :core:designsystem:recordRoborazziDebug
 * Verificar: ./gradlew :core:designsystem:verifyRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w407dp-h904dp-xxhdpi")
class GalleryScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(
        name: String,
        content: @Composable () -> Unit,
    ) {
        composeRule.setContent {
            WhTheme {
                Box(Modifier.fillMaxWidth().background(WhTheme.colors.background).padding(WhTheme.spacing.panelPadding)) { content() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun colors() = capture("colors") { GalleryColors() }

    @Test
    fun typography() = capture("typography") { GalleryTypography() }

    @Test
    fun icons() = capture("icons") { GalleryIcons() }

    @Test
    fun buttons() = capture("buttons") { GalleryButtons() }

    @Test
    fun fields() = capture("fields") { GalleryFields() }

    @Test
    fun panels() = capture("panels") { GalleryPanels() }

    @Test
    fun menu() = capture("menu") { GalleryMenu() }

    @Test
    fun lists() = capture("lists") { GalleryLists() }

    @Test
    fun bar() = capture("bar") { GalleryBar() }
}
