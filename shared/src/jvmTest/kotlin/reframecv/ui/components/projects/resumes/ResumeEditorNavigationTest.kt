package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.project.Project
import reframecv.domain.models.project.ProjectMode
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.sampleResumeContent
import reframecv.repository.MainResumeChange
import reframecv.repository.ProjectsRepository
import reframecv.repository.ResumesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.application.DefaultRootComponent
import reframecv.ui.components.application.RootComponent
import reframecv.ui.components.projects.resumes.editor.DefaultResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class ResumeEditorNavigationTest : ComponentTest() {
    @Test
    fun editorOwnsDraftAndCancelAndHardwareBackReturnToList() = runOnUiThread {
        lifecycle.resume()
        var backs = 0
        val component = DefaultResumesComponent(appComponentContext(), 7, back = { backs++ })
        component.onAdd()
        component.onAdd()
        val editor = assertIs<DefaultResumeEditorComponent>(component.editor)
        assertEquals(2, component.childStack.value.items.size)
        editor.onDraftChange(ResumeDraft("Android"))
        editor.saveState.value = ResumeSaveState.Saving
        editor.onDraftChange(ResumeDraft("Changed"))
        component.onBack()
        assertSame(editor, component.editor)
        assertEquals("Android", editor.draft.value.name)
        editor.saveState.value = ResumeSaveState.Idle
        (component.backHandler as BackDispatcher).back()
        assertNull(component.editor)
        assertEquals(0, backs)
        component.onAdd()
        assertEquals(ResumeDraft(), component.editor?.draft?.value)
        component.onBack()
        component.onBack()
        assertEquals(1, backs)
    }

    @Test
    fun failurePreservesDraftAndRetrySavesExactlyOnceToSelectedProject() = runBlocking<Unit> {
        val failure = IllegalStateException("Disk full")
        var fail = true
        var writes = 0
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repository = object : ResumesRepository by dependencies.resumesRepository {
            override suspend fun createSaved(
                record: Resume,
                mainResumeChange: MainResumeChange,
            ): Resume {
                writes++
                if (fail) throw failure
                entered.complete(Unit)
                release.await()
                return dependencies.resumesRepository.createSaved(record, mainResumeChange)
            }
        }
        lateinit var component: DefaultResumesComponent
        lateinit var editor: DefaultResumeEditorComponent
        val content = sampleResumeContent()
        val draft = structuredDraft()
        val failed = CompletableDeferred<Unit>()
        runOnUiThread {
            lifecycle.resume()
            component = DefaultResumesComponent(
                resumeContext(repository),
                7,
                back = {
                },
            )
            component.onAdd()
            editor = assertIs<DefaultResumeEditorComponent>(component.editor)
            editor.saveState.subscribe { if (it == ResumeSaveState.Failed) failed.complete(Unit) }
            editor.onSave()
            assertTrue(editor.validationRequested.value)
            assertEquals(0, writes)
            editor.onDraftChange(draft)
            editor.onSave()
        }
        withTimeout(10_000) {
            failed.await()
            val logged = dependencies.logger.failures.receive()
            assertEquals("Create resume", logged.operation)
            assertSame(failure, logged.error)
            runOnUiThread {
                assertSame(editor, component.editor)
                assertEquals(draft, editor.draft.value)
                fail = false
                editor.onSave()
                editor.onSave()
                component.onBack()
                assertSame(editor, component.editor)
            }
            entered.await()
            assertEquals(2, writes)
            finishSave(component, release)
            val record = dependencies.resumesRepository.records.value.single()
            assertEquals(7L, record.projectId)
            assertEquals(content, record.content)
        }
    }

    private suspend fun finishSave(
        component: DefaultResumesComponent,
        release: CompletableDeferred<Unit>,
    ) {
        val saved = CompletableDeferred<Unit>()
        runOnUiThread {
            checkNotNull(component.editor).saveState.subscribe {
                if (it == ResumeSaveState.Saved) saved.complete(Unit)
            }
        }
        release.complete(Unit)
        saved.await()
        runOnUiThread { assertEquals(ResumeSaveState.Saved, component.editor?.saveState?.value) }
    }

    private fun structuredDraft(): ResumeDraft {
        val content = sampleResumeContent()
        return content.sections.fold(ResumeDraft("Android", profile = content.profile)) {
                draft,
                section,
            ->
            draft.addSection(section)
        }
    }

    private fun resumeContext(repository: ResumesRepository) = DefaultAppComponentContext(
        DefaultComponentContext(lifecycle),
        object : ApplicationDependencies by dependencies {
            override val resumesRepository = repository
        },
    )

    @Test
    fun projectTreeCannotLeaveResumeCreationWhileSaving() = runBlocking<Unit> {
        val projects = listOf(1L, 2L).map {
            Project(it, "Project $it", createdAt = 0, updatedAt = 0, mode = ProjectMode.Workspace)
        }
        val services = object : ApplicationDependencies by dependencies {
            override val projectsRepository =
                object : ProjectsRepository by dependencies.projectsRepository {
                    override fun observeAllProjects() = flowOf(projects)
                }
        }
        val ready = CompletableDeferred<Unit>()
        lateinit var root: DefaultRootComponent
        runOnUiThread {
            lifecycle.resume()
            root =
                DefaultRootComponent(
                    DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
                )
            root.projectTree.state.subscribe { if (it.projects.size == 2) ready.complete(Unit) }
        }
        withTimeout(10_000) { ready.await() }
        runOnUiThread {
            root.projectTree.onProjectSelected(1)
            assertIs<RootComponent.Child.Dashboard>(
                root.childStack.value.active.instance,
            ).component.onOpenResumes()
            val resumes =
                assertIs<RootComponent.Child.Resumes>(root.childStack.value.active.instance)
            resumes.component.onAdd()
            val editor = assertIs<DefaultResumeEditorComponent>(resumes.component.editor)
            editor.saveState.value = ResumeSaveState.Saving
            root.onProjectsList()
            root.projectTree.onProjectSelected(2)
            (root.backHandler as BackDispatcher).back()
            assertSame(resumes, root.childStack.value.active.instance)
            assertSame(editor, resumes.component.editor)
            assertEquals(1L, root.projectTree.state.value.selectedId)
            editor.saveState.value = ResumeSaveState.Idle
            (root.backHandler as BackDispatcher).back()
            assertNull(resumes.component.editor)
            (root.backHandler as BackDispatcher).back()
            assertIs<RootComponent.Child.Dashboard>(root.childStack.value.active.instance)
        }
    }
}
