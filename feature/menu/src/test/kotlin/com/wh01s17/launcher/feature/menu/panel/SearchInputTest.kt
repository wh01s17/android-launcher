package com.wh01s17.launcher.feature.menu.panel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import com.wh01s17.launcher.core.designsystem.theme.WhTheme
import com.wh01s17.launcher.feature.menu.search.MenuSearchState
import com.wh01s17.launcher.feature.menu.search.SearchIntent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SearchInputTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editingKeepsEveryCharacterWhileSearchFeedbackIsDelayedAndExplicitResetStillWorks() {
        var state by mutableStateOf(MenuSearchState(open = true))
        var query = ""
        composeRule.setContent {
            WhTheme {
                LauncherMenu(MenuPage.Root, {}, {}, {}, {}, search = state, onSearch = {
                    if (it is SearchIntent.QueryChanged) query = it.query
                })
            }
        }
        val text = "Ir al escritorio 2"
        text.forEach { composeRule.onNode(hasSetTextAction()).performTextInput(it.toString()) }
        composeRule.onNode(hasSetTextAction()).assertTextEquals(text)
        assertEquals(text, query)
        composeRule.runOnIdle { state = state.copy(inputEpoch = 1, query = "") }
        composeRule.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
    }
}
