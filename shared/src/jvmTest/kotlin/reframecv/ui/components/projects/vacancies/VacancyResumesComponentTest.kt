package reframecv.ui.components.projects.vacancies

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.threading.runOnUiThread

class VacancyResumesComponentTest : ComponentTest() {
    @Test
    fun keepsLinksInDraftAndConfirmsUnlinkBeforeSaving() = runBlocking {
        dependencies.resumesRepository.records.value = listOf(
            resume(1, 7),
            resume(2, 7),
            resume(3, 8),
        )
        val loaded = CompletableDeferred<Unit>()
        val saved = mutableListOf<VacancyDraft>()
        var closes = 0
        lateinit var editor: DefaultVacancyEditorComponent
        runOnUiThread {
            lifecycle.resume()
            editor = DefaultVacancyEditorComponent(
                appComponentContext(),
                save = { saved += it },
                close = { closes++ },
                initialVacancy = Vacancy(
                    id = 1,
                    projectId = 7,
                    name = "Role",
                    createdAt = 0,
                    updatedAt = 0,
                    resumes = listOf(VacancyResume(1, 75)),
                ),
            )
            editor.resumes.state.subscribe { if (it.available.size == 2) loaded.complete(Unit) }
        }
        withTimeout(5000) { loaded.await() }
        runOnUiThread {
            editor.resumes.onSelect(1)
            editor.resumes.onSelect(3)
            editor.resumes.onSelect(-1)
            editor.resumes.onSelect(2)
            assertEquals(listOf(VacancyResume(1, 75), VacancyResume(2)), editor.draft.value.resumes)
            editor.resumes.onUnlink(1)
            editor.onSave(editor.draft.value)
            assertEquals(emptyList(), saved)
            editor.onClose()
            assertNull(editor.resumes.state.value.unlinkId)
            assertEquals(0, closes)
            editor.resumes.onUnlink(1)
            editor.resumes.onConfirmUnlink()
            editor.onSave(editor.draft.value)
            assertEquals(listOf(VacancyResume(2)), saved.single().resumes)
            editor.resumes.onSelect(1)
            assertEquals(listOf(VacancyResume(2)), editor.draft.value.resumes)
            editor.onSaveCompleted(VacancySaveState.Idle)
            editor.onClose()
            assertEquals(1, closes)
            assertEquals(3, dependencies.resumesRepository.records.value.size)
        }
    }

    private fun resume(id: Long, project: Long) = Resume(
        id = id,
        projectId = project,
        name = "Resume $id",
        createdAt = 0,
        updatedAt = 0,
    )
}
