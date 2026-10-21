package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyLettersState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyLettersContentTest {
    @Test
    fun filtersExistingLettersAttachesOpensAndConfirmsUnlink() = runDesktopComposeUiTest {
        val component = TestVacancyEditorComponent()
        component.letters.state.value =
            VacancyLettersState(
                available = listOf(
                    CoverLetter(1, 7, "First", 0, 0, keywords = listOf("Kotlin")),
                    CoverLetter(2, 7, "Other", 0, 0),
                ),
            )
        setContent {
            ReframeTheme {
                Column {
                    VacancyLettersContent(component, true)
                    VacancyLetterDialogs(component)
                }
            }
        }
        onNodeWithTag("vacancy.letters.generate").assertIsNotEnabled()
        onNodeWithTag("vacancy.letters.select").performClick().performTextReplacement("KoTL")
        onNodeWithTag("vacancy.letters.option.2").assertDoesNotExist()
        onNodeWithTag("vacancy.letters.option.1").performClick()
        assertEquals(listOf(1L), component.draft.value.coverLetterIds)
        onNodeWithTag("vacancy.letters.open.1").performClick()
        assertEquals(1L, component.letters.openedId)
        onNodeWithTag("vacancy.letters.unlink.1").performClick()
        onNode(
            hasText("Unlink") and hasAnyAncestor(hasTestTag("vacancy.letters.unlink.dialog")),
        ).performClick()
        assertEquals(emptyList(), component.draft.value.coverLetterIds)
        assertEquals(2, component.letters.state.value.available.size)
    }
}
