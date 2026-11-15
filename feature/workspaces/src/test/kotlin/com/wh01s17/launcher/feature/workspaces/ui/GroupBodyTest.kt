package com.wh01s17.launcher.feature.workspaces.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.apps.icons.IconRequest
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.config.GeneralConfig
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.feature.workspaces.TileInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Cambiar de pestaña en un grupo desliza el contenido en vez de saltar de golpe. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w407dp-h840dp-xxhdpi")
class GroupBodyTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val icons =
        object : AppIconLoader {
            override fun cached(request: IconRequest): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

            override suspend fun load(request: IconRequest): Bitmap = cached(request)
        }

    private fun tab(
        id: Long,
        name: String,
    ): Pair<TileId, TileInfo> {
        val key = AppKey("com.$name", "com.$name.Main", 0)
        return TileId(id) to TileInfo.App(key, AppEntry(key, name))
    }

    private val tabs = listOf(tab(1, "Claude"), tab(2, "ChatGPT"))

    @Test
    fun `switching tabs slides both contents before settling on the new one`() {
        var shown by mutableStateOf(TileId(1))
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            WhTheme {
                TileView(
                    info = tabs.first { it.first == shown }.second,
                    appsLoaded = true,
                    focused = true,
                    tabs = tabs,
                    shown = shown,
                    style = rememberTilingStyle(GeneralConfig()),
                    iconLoader = icons,
                    onSelectTab = {},
                    modifier = Modifier.size(300.dp),
                )
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        val restLeft = bodyLefts("Claude").single()
        composeRule.runOnUiThread { shown = TileId(2) }
        // Fotograma a fotograma y maquetando en cada uno, como en el teléfono (con el reloj manual, la
        // prueba sólo maqueta cuando se consulta el árbol).
        // Los nombres sólo cuentan mientras se ven dentro del tile (fuera, Compose los recorta a 0×0).
        var oldSlidLeft = false
        var newCameFromRight = false
        repeat(TRANSITION_FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            if (bodyLefts("Claude").any { it < restLeft }) oldSlidLeft = true
            if (bodyLefts("ChatGPT").any { it > restLeft }) newCameFromRight = true
        }
        assertTrue("the old tab did not slide out to the left", oldSlidLeft)
        assertTrue("the new tab did not slide in from the right", newCameFromRight)
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        // Al terminar sólo queda la nueva, en el sitio de la anterior.
        assertTrue(bodyLefts("Claude").isEmpty())
        assertEquals(restLeft, bodyLefts("ChatGPT").single())
    }

    /** Posición x de los nombres del cuerpo del tile (sin la pestaña, que está arriba). */
    private fun bodyLefts(text: String): List<Int> =
        composeRule
            .onAllNodes(hasText(text), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .filter { it.boundsInRoot.top > TABS_BOTTOM_PX }
            .map { it.boundsInRoot.left.toInt() }

    private companion object {
        /** Las pestañas ocupan la franja superior del tile; el nombre del cuerpo va mucho más abajo. */
        const val TABS_BOTTOM_PX = 200f
        const val TRANSITION_FRAMES = 30
    }
}
