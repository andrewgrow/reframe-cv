package reframecv.ui.compose.projects.resumes

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.resume.ResumeVacancyLink
import reframecv.ui.components.projects.resumes.ResumesState
import reframecv.ui.components.projects.resumes.TestResumesComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class ResumeVacancyLinksContentTest {
    @Test
    fun hundredVacanciesUseTwoPreviewLinksAndTheDialogCanOpenTheLastVacancy() =
        runDesktopComposeUiTest {
            val resume = resumeSamples().first()
            val opened = mutableListOf<Long>()
            val edits = mutableListOf<Long>()
            val component = TestResumesComponent(
                initialState = ResumesState.Ready(
                    listOf(resume),
                    linkedVacancies = List(100) {
                        ResumeVacancyLink(resume.id, it + 1L, "Vacancy ${it + 1}")
                    },
                ),
                edit = { edits += it.id },
                openVacancy = { opened += it },
            )
            setContent { ReframeTheme { ResumesContent(component) } }
            onNodeWithTag("resumes.link.${resume.id}.1").assertExists()
            onNodeWithTag("resumes.link.${resume.id}.2").assertExists()
            onNodeWithTag("resumes.link.${resume.id}.3").assertDoesNotExist()
            onNodeWithText("98 more").performClick()
            onNodeWithTag("resumes.links.dialog").assertExists()
            onNodeWithTag("resumes.link.${resume.id}.100").performScrollTo().performClick()
            onNodeWithTag("resumes.links.dialog").assertDoesNotExist()
            assertEquals(listOf(100L), opened)
            assertEquals(emptyList(), edits)
        }

    @Test
    fun closingTheDialogRetainsSelectionAndNoOverflowAppearsForTwoLinks() =
        runDesktopComposeUiTest {
            val resume = resumeSamples().first()
            val links = List(3) { ResumeVacancyLink(resume.id, it + 1L, "Vacancy ${it + 1}") }
            val component =
                TestResumesComponent(
                    initialState = ResumesState.Ready(listOf(resume), linkedVacancies = links),
                )
            setContent { ReframeTheme { ResumesContent(component) } }
            onNodeWithTag("resumes.select.${resume.id}").performClick()
            onNodeWithText("1 more").performClick()
            onNodeWithTag("resumes.links.close").performClick()
            onNodeWithTag("resumes.links.dialog").assertDoesNotExist()
            assertEquals(setOf(resume.id), component.selection.state.value.selected)
            runOnIdle {
                component.uiState.value =
                    ResumesState.Ready(listOf(resume), linkedVacancies = links.take(2))
            }
            onNodeWithTag("resumes.links.more.${resume.id}").assertDoesNotExist()
            onNodeWithTag("resumes.link.${resume.id}.2").assertExists()
        }
}
