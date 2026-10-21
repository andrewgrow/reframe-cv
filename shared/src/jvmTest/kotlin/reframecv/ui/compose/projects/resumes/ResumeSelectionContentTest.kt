package reframecv.ui.compose.projects.resumes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
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
import reframecv.domain.models.resume.ResumeVacancyLink
import reframecv.ui.components.projects.resumes.ResumesState
import reframecv.ui.components.projects.resumes.TestResumesComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class ResumeSelectionContentTest {
    @Test
    fun checkboxesNeverOpenEditorsAndEachVacancyLinkHasItsOwnDestination() =
        runDesktopComposeUiTest {
            val resume = resumeSamples().first()
            val edits = mutableListOf<Long>()
            val vacancies = mutableListOf<Long>()
            val component =
                TestResumesComponent(
                    initialState = ResumesState.Ready(
                        listOf(resume),
                        linkedVacancies = listOf(
                            ResumeVacancyLink(resume.id, 10, "ExampleCo Android"),
                            ResumeVacancyLink(resume.id, 11, "ExampleCo Mobile"),
                        ),
                    ),
                    edit = {
                        edits +=
                            it.id
                    },
                    openVacancy = { vacancies += it },
                )
            val narrow = androidx.compose.runtime.mutableStateOf(false)
            setContent {
                ReframeTheme {
                    Box(Modifier.width(if (narrow.value) 280.dp else 800.dp)) {
                        ResumesContent(component)
                    }
                }
            }
            listOf(false, true).forEach { size ->
                runOnIdle { narrow.value = size }
                onNodeWithTag("resumes.select.${resume.id}").performClick()
                assertEquals(emptyList(), edits)
                onNodeWithTag("resumes.link.${resume.id}.10").performClick()
                onNodeWithTag("resumes.link.${resume.id}.11").performClick()
            }
            assertEquals(listOf(10L, 11L, 10L, 11L), vacancies)
            onNodeWithText(resume.name).performClick()
            assertEquals(listOf(resume.id), edits)
        }

    @Test
    fun selectAllEnablesDeletionAndConfirmationShowsTheSelectedNames() = runDesktopComposeUiTest {
        val samples = resumeSamples()
        val component = TestResumesComponent(initialState = ResumesState.Ready(samples))
        setContent { ReframeTheme { ResumesContent(component) } }
        onNodeWithTag("resumes.delete.selected").assertIsNotEnabled()
        onNodeWithTag("resumes.select.all").performClick()
        assertEquals(samples.map { it.id }.toSet(), component.selection.state.value.selected)
        onNodeWithTag("resumes.delete.selected").assertIsEnabled().performClick()
        onNodeWithTag("resumes.delete.dialog").assertExists()
        onNode(
            hasText("Cancel") and hasAnyAncestor(hasTestTag("resumes.delete.dialog")),
        ).performClick()
        onNodeWithTag("resumes.delete.dialog").assertDoesNotExist()
        assertEquals(samples.map { it.id }.toSet(), component.selection.state.value.selected)
    }
}
