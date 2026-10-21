package reframecv.ui.compose.projects.coverletters

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.ui.components.projects.coverletters.TestCoverLettersComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class CoverLetterListContentTest {
    @Test
    fun selectionAndSourceLinksDoNotOpenLetterEditorsInEitherLayout() = runDesktopComposeUiTest {
        val edits = mutableListOf<Long>()
        val sources = mutableListOf<Long>()
        val component = TestCoverLettersComponent(
            initialState = letterListSamples(),
            edit = { edits += it.id },
            openResume = { sources += it },
        )
        val narrow = mutableStateOf(false)
        setContent {
            ReframeTheme {
                Box(Modifier.width(if (narrow.value) 280.dp else 800.dp)) {
                    CoverLettersContent(component)
                }
            }
        }
        listOf(false, true).forEach { size ->
            runOnIdle { narrow.value = size }
            onNodeWithTag("coverletters.select.1").performClick()
            onNodeWithTag("coverletters.link.1.10").performClick()
        }
        assertEquals(listOf(10L, 10L), sources)
        assertEquals(emptyList(), edits)
        onNodeWithText(letterListSamples().letters.first().name).performClick()
        assertEquals(listOf(1L), edits)
    }

    @Test
    fun selectAllEnablesBulkConfirmationAndCancelRetainsSelection() = runDesktopComposeUiTest {
        val component = TestCoverLettersComponent(initialState = letterListSamples())
        setContent { ReframeTheme { CoverLettersContent(component) } }
        onNodeWithTag("coverletters.delete.selected").assertIsNotEnabled()
        onNodeWithTag("coverletters.select.all").performClick()
        onNodeWithTag("coverletters.delete.selected").performClick()
        onNodeWithTag("coverletters.delete.dialog").assertExists()
        onNode(hasText("Cancel") and hasAnyAncestor(hasTestTag("coverletters.delete.dialog")))
            .performClick()
        onNodeWithTag("coverletters.delete.dialog").assertDoesNotExist()
        assertEquals(setOf(1L, 2L, 3L), component.selection.state.value.selected)
    }
}
