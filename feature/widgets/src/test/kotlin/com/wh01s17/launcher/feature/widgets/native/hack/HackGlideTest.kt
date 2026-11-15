package com.wh01s17.launcher.feature.widgets.native.hack

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
class HackGlideTest {
    @get:Rule val compose = createComposeRule()

    private fun x(text: String) =
        compose
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .layoutInfo.coordinates
            .positionInRoot()
            .x

    @Test
    fun `a line that does not fit glides to its end and back while a short one stays put`() {
        val long = "Shine On You Crazy Diamond (Parts I-V) [2011 Remastered Version]"
        compose.mainClock.autoAdvance = false
        compose.setContent {
            WhTheme {
                Column(Modifier.width(200.dp)) {
                    val glide = rememberHackGlide(long, "Pink Floyd")
                    HackGlideLine(glide, "track", long)
                    HackGlideLine(glide, "artist", "Pink Floyd")
                }
            }
        }
        compose.mainClock.advanceTimeBy(FRAME)
        val start = x(long)
        val artist = x("Pink Floyd")
        // Quieto unos dos segundos, luego un carácter cada 0,3 s.
        compose.mainClock.advanceTimeBy(1_500)
        assertEquals(start, x(long))
        compose.mainClock.advanceTimeBy(2_000)
        val moved = x(long)
        assertTrue("start=$start moved=$moved", moved < start)
        assertEquals("la corta no se mueve", artist, x("Pink Floyd"))
        // Sigue hasta el final (se para allí) y vuelve al principio.
        val trail =
            generateSequence { compose.mainClock.advanceTimeBy(STEP).let { x(long) } }
                .take((TRIP / STEP).toInt())
                .toList()
        val end = trail.min()
        assertTrue("se para en el final", trail.count { it == end } > 1)
        assertEquals("vuelve al principio", start, trail.drop(trail.indexOf(end)).first { it > end + 1 && it == start })
    }

    @Test
    fun `a line that fits never moves`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            WhTheme {
                Column(Modifier.width(200.dp)) {
                    HackGlideLine(rememberHackGlide("Opeth"), "artist", "Opeth")
                }
            }
        }
        compose.mainClock.advanceTimeBy(FRAME)
        val start = x("Opeth")
        compose.mainClock.advanceTimeBy(10_000)
        assertEquals(start, x("Opeth"))
    }

    private companion object {
        const val FRAME = 16L
        const val STEP = 100L

        /** De sobra para ir hasta el final de la línea larga, esperar y volver (≈ 2 × 60 × 0,3 s + pausas). */
        const val TRIP = 60_000L
    }
}
