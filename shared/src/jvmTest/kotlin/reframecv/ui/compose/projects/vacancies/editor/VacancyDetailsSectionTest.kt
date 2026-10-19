package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyDetailsSectionTest {
    @Test
    fun collapsingPreservesContentsAndResizedHeightAndSaveIncludesHiddenFields() =
        runDesktopComposeUiTest {
            val saved = mutableListOf<VacancyDraft>()
            val editor = TestVacancyEditorComponent(save = { saved += it })
            editor.draft.value = VacancyDraft(
                "Role",
                description = "Description",
                responsibilities = listOf("Build"),
                requirements = listOf("Kotlin"),
                niceToHave = listOf("German"),
            )
            val original = editor.draft.value
            setContent { ReframeTheme { VacancyEditorContent(editor) } }
            onNodeWithTag("vacancy.description.resize").performScrollTo().performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(0f, 15f))
                moveBy(Offset(0f, 45f))
                release()
            }
            val height = onNodeWithTag("vacancy.description").fetchSemanticsNode().size.height
            onNodeWithTag("vacancy.details.toggle").performScrollTo().performClick()
            waitForIdle()
            listOf("description", "responsibilities", "requirements", "niceToHave").forEach {
                onNodeWithTag("vacancy.$it").assertDoesNotExist()
            }
            onNodeWithTag("vacancy.url").assertExists()
            onNodeWithTag("vacancy.keywords").assertExists()
            onNodeWithTag("vacancy.details.toggle").performClick()
            waitForIdle()
            assertEquals(
                height,
                onNodeWithTag("vacancy.description").fetchSemanticsNode().size.height,
            )
            assertEquals(original, editor.draft.value)
            onNodeWithTag("vacancy.details.toggle").performClick()
            waitForIdle()
            onNodeWithText("Save").performClick()
            assertEquals(listOf(original), saved)
        }
}
