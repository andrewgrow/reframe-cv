package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.ui.components.projects.vacancies.editor.LetterResumeSelection
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyResumesState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class LetterResumeSelectionContentTest {
    @Test
    fun cancelAndGenerateDoNotOverlapAndSelectionEnablesGeneration() = runDesktopComposeUiTest {
        val component = letterResumeSelectionFixture(selected = null)
        setContent { ReframeTheme { VacancyLetterDialogs(component) } }
        val cancel = onNodeWithTag("vacancy.letters.source.cancel")
        val generate = onNodeWithTag("vacancy.letters.continue")
        val cancelBounds = cancel.fetchSemanticsNode().boundsInRoot
        val generateBounds = generate.fetchSemanticsNode().boundsInRoot
        assertTrue(!cancelBounds.overlaps(generateBounds))
        generate.assertIsNotEnabled()
        onNodeWithTag("vacancy.letters.source.2").performClick()
        generate.assertIsEnabled()
        assertEquals(2L, component.letterGeneration.selection.value.resumeId)
        cancel.performClick()
        onNodeWithTag("vacancy.letters.source.dialog").assertDoesNotExist()
    }
}

internal fun letterResumeSelectionFixture(selected: Long? = 2) = TestVacancyEditorComponent().also {
    it.draft.value = it.draft.value.copy(resumes = listOf(VacancyResume(1), VacancyResume(2)))
    it.resumes.state.value = VacancyResumesState(
        available = listOf(
            Resume(1, 7, "Main resume", 0, 0),
            Resume(2, 7, "Main resume + ExampleCo (AI 2026-10-21 18:15)", 0, 0),
        ),
    )
    it.letterGeneration.selection.value = LetterResumeSelection(true, selected)
}
