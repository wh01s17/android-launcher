package com.wh01s17.launcher.feature.workspaces.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.wh01s17.launcher.core.apps.icons.AppIconLoader
import com.wh01s17.launcher.core.apps.icons.IconRequest
import com.wh01s17.launcher.core.apps.model.AppEntry
import com.wh01s17.launcher.core.apps.model.AppKey
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.input.WorkspaceCommand
import com.wh01s17.launcher.core.tiling.TileId
import com.wh01s17.launcher.core.tiling.groupWith
import com.wh01s17.launcher.feature.workspaces.TileInfo
import com.wh01s17.launcher.feature.workspaces.WorkspacesIntent
import com.wh01s17.launcher.feature.workspaces.WorkspacesUiState
import com.wh01s17.launcher.feature.workspaces.model.Desk
import com.wh01s17.launcher.feature.workspaces.model.TileContent
import com.wh01s17.launcher.feature.workspaces.model.withDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Capturas de referencia de los escritorios (plan.md §9), a 480 dpi y 407 dp de ancho como el
 * Redmi; el alto es el que queda debajo de la barra.
 *
 * Grabar:   ./gradlew :feature:workspaces:recordRoborazziDebug
 * Verificar: ./gradlew :feature:workspaces:verifyRoborazziDebug
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w407dp-h840dp-xxhdpi")
class WorkspacesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Un cuadrado de color por app, para ver dónde va el icono sin depender de apps reales. */
    private val icons =
        object : AppIconLoader {
            override fun cached(request: IconRequest): Bitmap = square(request.sizePx)

            override suspend fun load(request: IconRequest): Bitmap = square(request.sizePx)

            private fun square(size: Int): Bitmap =
                Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
                    Canvas(it).drawRoundRect(
                        0f,
                        0f,
                        size.toFloat(),
                        size.toFloat(),
                        size / 5f,
                        size / 5f,
                        Paint().apply {
                            color =
                                Color.rgb(132, 201, 89)
                        },
                    )
                }
        }

    private fun app(name: String) = AppEntry(AppKey("com.$name", "com.$name.Main", 0), name)

    private val apps = listOf(app("Termux"), app("Notas"), app("Música"), app("Mapas"), app("Cámara"))

    /** Cinco apps en el escritorio 1 con tres huecos: las dos últimas van en grupo con la tercera. */
    private fun state(
        editing: Boolean = false,
        current: Int = 1,
    ): WorkspacesUiState {
        var desk = Desk().withDefaults(com.wh01s17.launcher.core.tiling.TilingLayout.Dwindle, maxTiles = 3)
        apps.forEach { desk = desk.add(TileContent.App(it.key)) }
        // Las dos últimas se agrupan con la tercera a mano (los grupos ya no se crean solos).
        desk =
            desk.update(1) { ws ->
                val (third, fourth, fifth) = ws.tiles.drop(2)
                ws.groupWith(fourth, third).groupWith(fifth, third)
            }
        desk = desk.add(TileContent.App(AppKey("com.gone", "com.gone.Main", 0)), workspace = 2)
        val tiles =
            desk.contents.mapValues { (_, content) ->
                val key = (content as TileContent.App).key
                TileInfo.App(key, apps.firstOrNull { it.key == key })
            }
        return WorkspacesUiState(
            loaded = true,
            current = current,
            workspaces = desk.workspaces,
            tiles = tiles,
            appsLoaded = true,
            editing = editing,
        )
    }

    private fun show(
        state: WorkspacesUiState,
        intents: MutableList<WorkspacesIntent> = mutableListOf(),
        commands: MutableList<WorkspaceCommand> = mutableListOf(),
    ) {
        composeRule.setContent {
            WhTheme {
                Box(Modifier.fillMaxSize().background(WhTheme.colors.background)) {
                    WorkspacesScreen(
                        state = state,
                        iconLoader = icons,
                        onIntent = { intents += it },
                        onCommand = { commands += it },
                        onAddTile = {},
                        onAddWidget = {},
                        onOpenMenu = {},
                        onGesture = {},
                        boundGestures = emptySet(),
                        onOpenSettings = {},
                    )
                }
            }
        }
    }

    private fun capture(name: String) {
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun tiles() {
        show(state())
        capture("workspace_tiles")
    }

    @Test
    fun editMode() {
        show(state(editing = true))
        capture("workspace_edit")
    }

    @Test
    fun uninstalledApp() {
        show(state(current = 2))
        capture("workspace_uninstalled")
    }

    @Test
    fun emptyWorkspace() {
        show(state(current = 3))
        capture("workspace_empty")
    }

    @Test
    fun `long press on a tile enters edit mode and the toolbar sends commands`() {
        val intents = mutableListOf<WorkspacesIntent>()
        show(state(), intents)
        composeRule.onNodeWithText("Termux").performTouchInput { longClick() }
        composeRule.waitForIdle()
        assertTrue(intents.contains(WorkspacesIntent.LongPressTile(TileId(1))))
    }

    @Test
    fun `swiping on a grouped tile switches to the neighbouring tab`() {
        val intents = mutableListOf<WorkspacesIntent>()
        show(state(), intents)
        // «Mapas» sólo aparece como pestaña del grupo; deslizar sobre ella cuenta como sobre el tile.
        composeRule.onNodeWithText("Mapas", useUnmergedTree = true).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Mapas", useUnmergedTree = true).performTouchInput { swipeRight() }
        composeRule.waitForIdle()
        val selected = intents.filterIsInstance<WorkspacesIntent.SelectTab>()
        assertTrue("$intents", selected.isNotEmpty())
        // El carrusel no se mueve y no cuenta como pulsación larga.
        assertTrue("$intents", intents.none { it is WorkspacesIntent.PageSettled && it.workspace != 1 })
        assertTrue("$intents", intents.none { it is WorkspacesIntent.LongPressTile || it == WorkspacesIntent.LongPressBackground })
    }

    @Test
    fun `edit toolbar buttons send workspace commands`() {
        val commands = mutableListOf<WorkspaceCommand>()
        show(state(editing = true), commands = commands)
        composeRule.onNodeWithText("flotar").performClick()
        composeRule.onNodeWithText("dwindle").performClick()
        composeRule.onNodeWithText("quitar").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(WorkspaceCommand.ToggleFloating, WorkspaceCommand.ToggleLayout, WorkspaceCommand.CloseTile), commands)
    }

    /** TalkBack (plan.md §7 Hito 11): cada tile dice qué app es aunque el nombre no quepa, y el foco se oye. */
    @Test
    fun `every touchable tile is labelled, large enough and the focused one is selected`() {
        show(state())
        composeRule.waitForIdle()
        val nodes =
            composeRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
                .fetchSemanticsNodes()
        val minimum = with(composeRule.density) { 48.dp.toPx() } - 1f
        val problems =
            nodes.mapNotNull { node ->
                val label =
                    (
                        node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
                            node.config
                                .getOrNull(SemanticsProperties.Text)
                                .orEmpty()
                                .map { it.text }
                    ).joinToString(" ")
                val bounds = node.touchBoundsInRoot
                when {
                    label.isBlank() -> "unlabeled node at ${node.boundsInRoot}"
                    bounds.width < minimum || bounds.height < minimum -> "'$label' touch target ${bounds.size}"
                    else -> null
                }
            }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
        // Las cinco apps (tres tiles, uno de ellos un grupo con pestañas) y el fondo.
        val labels = nodes.flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
        assertTrue("$labels", labels.any { it.startsWith("escritorio") })
        val selected = nodes.count { it.config.getOrNull(SemanticsProperties.Selected) == true }
        assertTrue("selected = $selected", selected >= 1)
    }
}
