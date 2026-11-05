package com.wh01s17.launcher.core.designsystem.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WhNumberFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var value by mutableIntStateOf(100)

    private fun show(enabled: Boolean = true) {
        composeRule.setContent {
            WhTheme { WhNumberField(value, onValue = { value = it }, enabled = enabled) }
        }
    }

    @Test
    fun `tapping lets you type a number and enter applies it`() {
        show()
        composeRule.onNodeWithText("100").performClick()
        composeRule.onNodeWithText("100").performTextReplacement("128")
        composeRule.onNodeWithText("128").performImeAction()
        composeRule.waitForIdle()
        assertEquals(128, value)
        // Vuelve a ser texto, con el valor nuevo.
        composeRule.onNodeWithText("128").assertExists()
    }

    @Test
    fun `only digits are accepted`() {
        show()
        composeRule.onNodeWithText("100").performClick()
        composeRule.onNodeWithText("100").performTextReplacement("9a")
        composeRule.onNodeWithText("100").performImeAction()
        assertEquals(100, value)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `escape cancels`() {
        show()
        composeRule.onNodeWithText("100").performClick()
        composeRule.onNodeWithText("100").performTextReplacement("60")
        composeRule.onNodeWithText("60").performKeyInput { pressKey(Key.Escape) }
        composeRule.waitForIdle()
        assertEquals(100, value)
        composeRule.onNodeWithText("100").assertExists()
    }
}
