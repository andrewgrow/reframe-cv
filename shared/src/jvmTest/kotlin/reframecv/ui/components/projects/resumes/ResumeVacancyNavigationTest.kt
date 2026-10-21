package reframecv.ui.components.projects.resumes

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.VacanciesComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.threading.runOnUiThread

class ResumeVacancyNavigationTest : ComponentTest() {
    @Test
    fun opensTheLinkedVacancyAndBackReturnsToTheSelectedResumeList() = runBlocking<Unit> {
        dependencies.resumesRepository.records.value = listOf(Resume(1, 7, "Main", 0, 0))
        dependencies.vacanciesRepository.records.value =
            listOf(
                Vacancy(
                    id = 2,
                    projectId = 7,
                    name = "ExampleCo",
                    createdAt = 0,
                    updatedAt = 0,
                    resumes = listOf(VacancyResume(1)),
                ),
            )
        val ready = CompletableDeferred<Unit>()
        val opened = CompletableDeferred<VacancyEditorComponent>()
        val component = runOnUiThread {
            lifecycle.resume()
            DefaultResumesComponent(appComponentContext(), 7, back = {
                error("Must return to resume list")
            }).also { host ->
                host.uiState.subscribe {
                    if (it is ResumesState.Ready &&
                        it.linkedVacancies.isNotEmpty()
                    ) {
                        ready.complete(Unit)
                    }
                }
                host.childStack.subscribe { stack ->
                    val target = stack.active.instance as? ResumesComponent.Child.LinkedVacancy
                    target?.component?.childStack?.subscribe {
                        val page = it.active.instance as? VacanciesComponent.Child.Editor
                        val editor = page?.component
                        if (editor != null) opened.complete(editor)
                    }
                }
            }
        }
        withTimeout(5000) { ready.await() }
        runOnUiThread {
            component.selection.onToggle(1)
            component.onOpenVacancy(999)
            assertIs<ResumesComponent.Child.List>(component.childStack.value.active.instance)
            component.onOpenVacancy(2)
        }
        val editor = withTimeout(5000) { opened.await() }
        runOnUiThread {
            assertEquals(2L, editor.initialVacancy!!.id)
            editor.onDraftChange(editor.draft.value.copy(name = "Unsaved"))
            component.onBack()
            assertIs<ResumesComponent.Child.List>(component.childStack.value.active.instance)
            assertEquals(setOf(1L), component.selection.state.value.selected)
            assertEquals("ExampleCo", dependencies.vacanciesRepository.records.value.single().name)
        }
    }
}
