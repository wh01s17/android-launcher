package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w407dp-h904dp-xxhdpi")
class HackSeekTest {
    @get:Rule val compose = createComposeRule()
    private val seeks = mutableListOf<Float>()

    private fun meter(onSeek: ((Float) -> Unit)?) =
        compose.setContent {
            WhTheme {
                Column(Modifier.width(300.dp).testTag("meter")) {
                    HackMeter("", 0.1f, "0:30/5:00", WhTheme.colors.accent, onSeek = onSeek)
                }
            }
        }

    @Test
    fun `tapping the bar seeks to that point and the label and time are not part of it`() {
        meter { seeks += it }
        compose.onNodeWithTag("meter").performTouchInput { click(centerLeft) }
        compose.onNodeWithTag("meter").performTouchInput { click(centerRight) }
        // El borde izquierdo es el `[`: antes de la barra, cuenta como el principio; el derecho es el
        // tiempo, después de la barra: el final.
        assertEquals(listOf(0f, 1f), seeks)
    }

    @Test
    fun `dragging seeks once, on release, where the finger stops`() {
        meter { seeks += it }
        compose.onNodeWithTag("meter").performTouchInput {
            down(centerLeft)
            moveBy(
                androidx.compose.ui.geometry
                    .Offset(width * 0.25f, 0f),
            )
            moveBy(
                androidx.compose.ui.geometry
                    .Offset(width * 0.25f, 0f),
            )
            up()
        }
        assertEquals(1, seeks.size)
        // A media fila: la barra acaba antes que la fila (el tiempo va a la derecha), así que pasa del medio.
        assertTrue("seek=${seeks.single()}", seeks.single() in 0.55f..0.95f)
    }

    @Test
    fun `without a seek action the bar ignores touches`() {
        meter(null)
        compose.onNodeWithTag("meter").performClick()
        assertTrue(seeks.isEmpty())
    }
}
