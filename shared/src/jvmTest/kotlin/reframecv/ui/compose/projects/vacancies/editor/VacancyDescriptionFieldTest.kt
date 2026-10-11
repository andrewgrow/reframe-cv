package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyDescriptionFieldTest {
    @Test
    fun mouseResizeGrowsAndShrinksWithoutChangingDescription() = runComposeUiTest {
        setContent { ReframeTheme { VacancyEditorContent(TestVacancyEditorComponent()) } }
        val field = onNodeWithTag("vacancy.description")
        field.performTextReplacement("Original description")
        val minimum = field.fetchSemanticsNode().boundsInRoot.height
        onNodeWithTag("vacancy.description.resize").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(0f, 20f))
            moveBy(Offset(0f, 80f))
            release()
        }
        val expandedHeight = field.fetchSemanticsNode().size.height
        assertTrue(expandedHeight > minimum + 50)
        field.performTextReplacement("Updated description")
        assertEquals(expandedHeight, field.fetchSemanticsNode().size.height)
        onNodeWithTag("vacancy.description.resize").performScrollTo().performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(0f, -20f))
            moveBy(Offset(0f, -400f))
            release()
        }
        assertTrue(field.fetchSemanticsNode().size.height <= minimum + 1)
        onNodeWithText("Updated description").assertExists()
    }

    @Test
    fun holdingHandleAtViewportEdgeContinuesGrowingAndScrolling() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            setContent { ReframeTheme { VacancyEditorContent(TestVacancyEditorComponent()) } }
            mainClock.autoAdvance = false
            val handle = onNodeWithTag("vacancy.description.resize")
            val fields = onNodeWithTag("vacancy.fields")
            val bottom = fields.fetchSemanticsNode().boundsInRoot.bottom
            val start = handle.fetchSemanticsNode().boundsInRoot.center.y
            handle.performMouseInput {
                moveTo(center)
                press()
                moveBy(Offset(0f, 10f))
                moveBy(Offset(0f, bottom - start - 12f))
            }
            mainClock.advanceTimeBy(100)
            val height = onNodeWithTag("vacancy.description").fetchSemanticsNode().size.height
            mainClock.advanceTimeBy(2400)
            val expanded = onNodeWithTag("vacancy.description").fetchSemanticsNode().size.height
            assertTrue(expanded > 600, "Description must grow beyond the viewport height")
            fields.performMouseInput { release() }
            mainClock.autoAdvance = true
            assertTrue(
                expanded > height + 20,
                "height=$height expanded=$expanded bottom=$bottom start=$start",
            )
            onNodeWithText("Cancel").assertExists()
            onNodeWithText("Attach source file").performScrollTo().assertExists()
        }

    @Test
    fun deleteConfirmationDisablesResizing() = runComposeUiTest {
        val vacancy = Vacancy(id = 1, projectId = 7, name = "Backend", createdAt = 0, updatedAt = 0)
        setContent {
            ReframeTheme {
                VacancyEditorContent(TestVacancyEditorComponent(initialVacancy = vacancy))
            }
        }
        onNodeWithText("Delete").performClick()
        onNodeWithTag("vacancy.description.resize").assertIsNotEnabled()
    }
}
