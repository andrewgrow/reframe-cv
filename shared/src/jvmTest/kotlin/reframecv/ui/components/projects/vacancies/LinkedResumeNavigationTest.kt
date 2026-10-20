package reframecv.ui.components.projects.vacancies

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.resumes.editor.ResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeRole
import reframecv.ui.threading.runOnUiThread

class LinkedResumeNavigationTest : ComponentTest() {
    @Test
    fun opensLinkedResumeAndReturnsToTheSameVacancyDraft() = runBlocking {
        val resume = Resume(id = 1, projectId = 7, name = "Main", createdAt = 0, updatedAt = 0)
        dependencies.resumesRepository.records.value = listOf(resume)
        dependencies.resumesRepository.mainResumeIds.value = mapOf(7L to 1L)
        val loaded = CompletableDeferred<Unit>()
        val opened = CompletableDeferred<ResumeEditorComponent>()
        lateinit var component: DefaultVacanciesComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacanciesComponent(appComponentContext(), 7, back = {})
            component.onEdit(
                Vacancy(
                    id = 2,
                    projectId = 7,
                    name = "Vacancy",
                    createdAt = 0,
                    updatedAt = 0,
                    resumes = listOf(VacancyResume(1)),
                ),
            )
            component.editor!!.resumes.state.subscribe {
                if (it.available.isNotEmpty()) loaded.complete(Unit)
            }
            component.childStack.subscribe {
                val child = it.active.instance as? VacanciesComponent.Child.LinkedResume
                child?.component?.childStack?.subscribe { stack ->
                    val editor = (
                        stack.active.instance as?
                            reframecv.ui.components.projects.resumes.ResumesComponent.Child.Editor
                        )
                        ?.component
                    if (editor != null) opened.complete(editor)
                }
            }
        }
        withTimeout(5000) { loaded.await() }
        val vacancyEditor = component.editor!!
        runOnUiThread {
            vacancyEditor.onDraftChange(vacancyEditor.draft.value.copy(name = "Unsaved change"))
            vacancyEditor.resumes.onOpen(1)
        }
        val resumeEditor = withTimeout(5000) { opened.await() }
        runOnUiThread {
            assertEquals(1L, resumeEditor.initialResume!!.id)
            assertEquals(ResumeRole.Main, resumeEditor.draft.value.role)
            component.onBack()
            assertSame(vacancyEditor, component.editor)
            assertEquals("Unsaved change", component.editor!!.draft.value.name)
        }
    }
}
