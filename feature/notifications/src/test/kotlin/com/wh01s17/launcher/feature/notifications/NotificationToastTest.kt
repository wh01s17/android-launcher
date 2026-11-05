package com.wh01s17.launcher.feature.notifications

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.notifications.NotificationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun item(
    key: String,
    title: String = key,
) = NotificationItem(key, "com.chat", 0, title, "texto", 0, clearable = true, autoCancel = true, open = null)

class NewestSinceTest {
    @Test
    fun `the first reading never counts as new`() {
        assertNull(newestSince(null, listOf(item("a"), item("b"))))
    }

    @Test
    fun `a key that was not known is the new one`() {
        assertEquals("c", newestSince(setOf("a", "b"), listOf(item("c"), item("a")))?.key)
    }

    @Test
    fun `nothing new when the keys are the same or fewer`() {
        assertNull(newestSince(setOf("a", "b"), listOf(item("a"))))
        assertNull(newestSince(setOf("a"), listOf(item("a"))))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w407dp-h904dp-xxhdpi", manifest = Config.NONE)
class NotificationToastTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val items = mutableStateOf(listOf(item("a", "Ana")))
    private val enabled = mutableStateOf(true)
    private val opened = mutableListOf<String>()

    private fun show() {
        composeRule.setContent {
            WhTheme {
                NotificationToastHost(items.value, enabled.value, seconds = 5, labelOf = { "Chat" }, onOpen = { opened += it.key })
            }
        }
    }

    @Test
    fun `a notification that was already there is not announced`() {
        show()
        composeRule.onNodeWithText("Ana").assertDoesNotExist()
    }

    @Test
    fun `a new notification is announced and tapping it opens it`() {
        show()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { items.value = listOf(item("b", "Beto")) + items.value }
        // Robolectric needs pending snapshot writes applied before advancing the frame clock.
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Beto").assertExists()
        composeRule.onNodeWithText("Beto").performClick()
        assertEquals(listOf("b"), opened)
    }

    @Test
    fun `with the toast off nothing is announced`() {
        enabled.value = false
        show()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { items.value = listOf(item("b", "Beto")) + items.value }
        // Robolectric needs pending snapshot writes applied before advancing the frame clock.
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Beto").assertDoesNotExist()
    }

    @Test
    fun `the toast goes away when its countdown ends`() {
        show()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { items.value = listOf(item("b", "Beto")) + items.value }
        // Robolectric needs pending snapshot writes applied before advancing the frame clock.
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Beto").assertExists()
        composeRule.mainClock.advanceTimeBy(6_000)
        composeRule.onNodeWithText("Beto").assertDoesNotExist()
    }
}
