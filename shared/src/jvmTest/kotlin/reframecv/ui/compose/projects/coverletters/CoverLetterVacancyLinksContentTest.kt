package reframecv.ui.compose.projects.coverletters

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.coverletters.CoverLettersState
import reframecv.ui.components.projects.coverletters.TestCoverLettersComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class CoverLetterVacancyLinksContentTest {
    @Test
    fun multipleVacanciesStayCompactAndOpenIndependentlyOfTheLetter() = runDesktopComposeUiTest {
        val letter = letterListSamples().letters.first()
        val opened = mutableListOf<Long>()
        val edits = mutableListOf<Long>()
        val component = TestCoverLettersComponent(
            initialState = CoverLettersState.Ready(
                listOf(letter),
                List(100) {
                    Vacancy(
                        it + 1L,
                        1,
                        "Vacancy ${it + 1}",
                        0,
                        0,
                        coverLetterIds = listOf(letter.id),
                    )
                },
            ),
            openVacancy = { opened += it },
            edit = { edits += it.id },
        )
        setContent { ReframeTheme { CoverLettersContent(component) } }
        onNodeWithTag("coverletters.link.${letter.id}.1").assertExists()
        onNodeWithTag("coverletters.link.${letter.id}.2").assertExists()
        onNodeWithTag("coverletters.link.${letter.id}.3").assertDoesNotExist()
        onNodeWithText("98 more").performClick()
        onNodeWithTag("coverletters.links.close").performClick()
        onNodeWithTag("coverletters.links.dialog").assertDoesNotExist()
        onNodeWithText("98 more").performClick()
        onNodeWithTag("coverletters.link.${letter.id}.100").performScrollTo().performClick()
        onNodeWithTag("coverletters.links.dialog").assertDoesNotExist()
        assertEquals(listOf(100L), opened)
        assertEquals(emptyList(), edits)
    }
}
