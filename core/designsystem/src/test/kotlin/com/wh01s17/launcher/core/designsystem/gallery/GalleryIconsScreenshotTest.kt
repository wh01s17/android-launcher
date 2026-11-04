package com.wh01s17.launcher.core.designsystem.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
 * El catálogo de iconos en su propia captura: con 70 glifos no cabe en la ventana de 904 dp de las
 * demás y se cortaría (los nuevos quedarían fuera sin que la comparación lo notara). Misma
 * densidad y ancho que el Redmi, con más alto.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w407dp-h1600dp-xxhdpi")
class GalleryIconsScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun icons() {
        composeRule.setContent {
            WhTheme {
                Box(Modifier.fillMaxWidth().background(WhTheme.colors.background).padding(WhTheme.spacing.panelPadding)) { GalleryIcons() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/icons.png")
    }
}
