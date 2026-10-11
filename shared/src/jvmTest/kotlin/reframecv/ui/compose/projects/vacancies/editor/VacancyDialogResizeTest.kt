package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyDialogResizeTest {
    @Test
    fun resizesBothDimensionsAndReturnsToOriginalMinimum() =
        runDesktopComposeUiTest(width = 1200, height = 1000) {
            var closed = false
            val component = TestVacancyEditorComponent(close = { closed = true })
            setContent { ReframeTheme { VacancyEditorContent(component) } }
            val dialog = onNodeWithTag(VACANCY_EDITOR_TAG)
            val original = dialog.fetchSemanticsNode().size
            val handleSize = onNodeWithTag("vacancy.dialog.resize").fetchSemanticsNode().size
            assertTrue(handleSize.width <= 32 && handleSize.height <= 32)
            onNodeWithTag("vacancy.dialog.resize").performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(60f, 60f))
                release()
            }
            val expanded = dialog.fetchSemanticsNode().size
            assertTrue(expanded.width >= original.width + 100)
            assertTrue(expanded.height >= original.height + 100)
            onNodeWithTag("vacancy.title").performTextReplacement("Backend")
            assertEquals(expanded, dialog.fetchSemanticsNode().size)
            onNodeWithText("Save").assertExists()
            onNodeWithTag("vacancy.dialog.resize").performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(-300f, -300f))
                release()
            }
            assertEquals(original, dialog.fetchSemanticsNode().size)
            assertFalse(closed, "Releasing the resize handle must not dismiss the dialog")
        }

    @Test
    fun reachesWindowBoundsAndImmediatelyShrinksOnReverseDrag() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent { ReframeTheme { VacancyEditorContent(TestVacancyEditorComponent()) } }
            val handle = onNodeWithTag("vacancy.dialog.resize")
            handle.performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(500f, 500f))
                release()
            }
            val maximum = onNodeWithTag(VACANCY_EDITOR_TAG).fetchSemanticsNode().size
            assertEquals(800, maximum.width)
            assertEquals(600, maximum.height)
            handle.performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(-20f, 0f))
                release()
            }
            assertEquals(760, onNodeWithTag(VACANCY_EDITOR_TAG).fetchSemanticsNode().size.width)
        }

    @Test
    fun startsResizingFromCurrentSizeAfterDescriptionExpands() =
        runDesktopComposeUiTest(width = 1200, height = 1000) {
            setContent { ReframeTheme { VacancyEditorContent(TestVacancyEditorComponent()) } }
            val dialog = onNodeWithTag(VACANCY_EDITOR_TAG)
            val initialHeight = dialog.fetchSemanticsNode().size.height
            onNodeWithTag("vacancy.description.resize").performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(0f, 100f))
                release()
            }
            val grownHeight = dialog.fetchSemanticsNode().size.height
            assertTrue(grownHeight > initialHeight)
            onNodeWithTag("vacancy.dialog.resize").performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(0f, 20f))
                release()
            }
            assertEquals(grownHeight + 40, dialog.fetchSemanticsNode().size.height)
        }

    @Test
    fun busyAndConfirmingStatesDisableDialogResize() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            val vacancy =
                Vacancy(id = 1, projectId = 7, name = "Backend", createdAt = 0, updatedAt = 0)
            val component = TestVacancyEditorComponent(initialVacancy = vacancy)
            setContent { ReframeTheme { VacancyEditorContent(component) } }
            runOnIdle { component.saveState.value = VacancySaveState.Saving }
            onNodeWithTag("vacancy.dialog.resize").assertIsNotEnabled()
            runOnIdle { component.saveState.value = VacancySaveState.Idle }
            onNodeWithText("Delete").performClick()
            onNodeWithTag("vacancy.dialog.resize").assertIsNotEnabled()
        }
}
