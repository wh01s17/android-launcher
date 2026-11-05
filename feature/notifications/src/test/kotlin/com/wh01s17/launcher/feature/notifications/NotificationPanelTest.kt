package com.wh01s17.launcher.feature.notifications

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.notifications.NotificationItem
import com.wh01s17.launcher.core.system.notifications.NotificationState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w407dp-h904dp-xxhdpi", manifest = Config.NONE)
class NotificationPanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val now = 10 * 60_000L

    private fun item(
        key: String,
        title: String,
        text: String? = null,
        clearable: Boolean = true,
        minutesAgo: Long = 5,
    ) = NotificationItem(key, "com.chat", 0, title, text, now - minutesAgo * 60_000, clearable, autoCancel = true, open = null)

    private val opened = mutableListOf<String>()
    private val dismissed = mutableListOf<String>()
    private var cleared = 0
    private var granted = 0
    private var closed = 0

    private fun show(state: NotificationState) {
        composeRule.setContent {
            WhTheme {
                NotificationPanel(
                    state = state,
                    labelOf = { "Chat" },
                    now = now,
                    onOpen = { opened += it.key },
                    onDismiss = { dismissed += it.key },
                    onClearAll = { cleared++ },
                    onGrantAccess = { granted++ },
                    onClose = { closed++ },
                )
            }
        }
    }

    @Test
    fun `each notification shows its app, age, title and text`() {
        show(NotificationState(true, listOf(item("a", "Ana", "¿cenamos?"), item("b", "Luis", minutesAgo = 0))))
        composeRule.onNodeWithText("Chat · hace 5 min").assertIsDisplayed()
        composeRule.onNodeWithText("Ana").assertIsDisplayed()
        composeRule.onNodeWithText("¿cenamos?").assertIsDisplayed()
        composeRule.onNodeWithText("Chat · ahora").assertIsDisplayed()
        composeRule.onNodeWithText("2 notificaciones").assertIsDisplayed()
    }

    @Test
    fun `tapping a notification opens it`() {
        show(NotificationState(true, listOf(item("a", "Ana"), item("b", "Luis"))))
        composeRule.onNodeWithText("Luis").performClick()
        assertEquals(listOf("b"), opened)
    }

    @Test
    fun `the cross dismisses one notification and clear dismisses all`() {
        show(NotificationState(true, listOf(item("a", "Ana"), item("b", "Luis"))))
        composeRule.onAllNodesWithContentDescription("Descartar la notificación de Chat")[1].performClick()
        assertEquals(listOf("b"), dismissed)
        composeRule.onNodeWithText("limpiar").performClick()
        assertEquals(1, cleared)
    }

    @Test
    fun `a notification that cannot be cleared has no cross and does not enable clear`() {
        show(NotificationState(true, listOf(item("a", "Ana", clearable = false))))
        composeRule.onNodeWithContentDescription("Descartar la notificación de Chat").assertDoesNotExist()
        composeRule.onNodeWithText("limpiar").assertDoesNotExist()
    }

    @Test
    fun `without notifications it says so`() {
        show(NotificationState(true, emptyList()))
        composeRule.onNodeWithText("Sin notificaciones").assertIsDisplayed()
    }

    @Test
    fun `without access it explains and offers to grant it`() {
        show(NotificationState(access = false))
        composeRule.onNodeWithText("Falta el acceso a notificaciones").assertIsDisplayed()
        composeRule.onNodeWithText("activar acceso").performClick()
        assertEquals(1, granted)
        composeRule.onNodeWithText("Sin notificaciones").assertDoesNotExist()
    }

    @Test
    fun `tapping outside the panel closes it`() {
        show(NotificationState(true, emptyList()))
        composeRule.onNodeWithContentDescription("Cerrar las notificaciones").performClick()
        assertEquals(1, closed)
    }
}
