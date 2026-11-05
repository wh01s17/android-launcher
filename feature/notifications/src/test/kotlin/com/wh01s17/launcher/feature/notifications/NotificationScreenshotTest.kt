package com.wh01s17.launcher.feature.notifications

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.core.system.notifications.NotificationItem
import com.wh01s17.launcher.core.system.notifications.NotificationState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w407dp-h904dp-xxhdpi")
class NotificationScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun panel() {
        val item = NotificationItem("1", "com.chat", 0, "Ana", "¿cenamos?", 0, true, true, null)
        composeRule.setContent {
            WhTheme {
                NotificationPanel(
                    NotificationState(true, listOf(item)),
                    labelOf = { "Chat" },
                    now = 300_000,
                    onOpen = {},
                    onDismiss = {},
                    onClearAll = {},
                    onGrantAccess = {},
                    onClose = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/notifications-panel.png")
    }
}
