package reframecv.ui.compose.projects.resumes

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.projects.resumes.ResumesState
import reframecv.ui.components.projects.resumes.TestResumesComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.compose.common.LOADING_INDICATOR_TAG
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class ResumesContentTest {
    @Test
    fun showsNamesAndDatesAndKeepsAddAnchoredWhileScrolling() = runComposeUiTest {
        val sample = resumeSamples().first()
        val component = TestResumesComponent(
            initialState = ResumesState.Ready(
                List(30) { sample.copy(id = it.toLong(), name = "Resume $it") },
            ),
        )
        setContent { ReframeTheme { ResumesContent(component) } }
        onNodeWithText("Resume 0").assertIsDisplayed()
        onNodeWithTag(RESUMES_LIST_TAG).performScrollToNode(hasText("Resume 29"))
        onNodeWithText("Resume 29").assertIsDisplayed()
        onNodeWithText("Add").assertIsDisplayed().assertIsNotEnabled()
        runOnIdle { component.uiState.value = ResumesState.Ready(listOf(sample)) }
        onNodeWithText(sample.name).assertIsDisplayed()
        onNodeWithText("2026-10-10").assertIsDisplayed()
        onNodeWithText("Added").assertIsDisplayed()
    }

    @Test
    fun loadingIsDelayedAndFailureRetryIsInTheFooter() = runComposeUiTest {
        mainClock.autoAdvance = false
        var retries = 0
        val component = TestResumesComponent(
            initialState = ResumesState.Loading,
            retry = { retries++ },
        )
        setContent { ReframeTheme { ResumesContent(component) } }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(900)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertDoesNotExist()
        onNodeWithText("You have no resumes yet.").assertDoesNotExist()
        mainClock.advanceTimeBy(200)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertIsDisplayed()
        runOnIdle { component.uiState.value = ResumesState.LoadFailed }
        mainClock.advanceTimeByFrame()
        onNodeWithText("Could not load resumes. Please try again.").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
        assertEquals(
            onNodeWithText("Add").fetchSemanticsNode().boundsInRoot.top,
            onNodeWithText("Retry").fetchSemanticsNode().boundsInRoot.top,
        )
        runOnIdle { component.uiState.value = ResumesState.Ready(emptyList()) }
        mainClock.advanceTimeByFrame()
        onNodeWithText("You have no resumes yet.").assertIsDisplayed()
        onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun arrowAndEscapeWorkWhenProjectNavigationHasFocus() = runComposeUiTest {
        var backs = 0
        val component = TestResumesComponent(back = { backs++ })
        setContent { ReframeTheme { RootContent(TestRootComponent(resumesComponent = component)) } }
        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
        onNodeWithText("Projects").performClick()
        onNodeWithTag("app.root").performKeyInput { pressKey(Key.Escape) }
        assertEquals(2, backs)
    }
}
