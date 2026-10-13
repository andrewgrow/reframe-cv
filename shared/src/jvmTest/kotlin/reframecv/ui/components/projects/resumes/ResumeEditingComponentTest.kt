package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.sampleResumeContent
import reframecv.repository.ResumesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.resumes.editor.DefaultResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class ResumeEditingComponentTest : ComponentTest() {
    private fun original() = Resume(
        id = 12,
        projectId = 7,
        name = "Mobile",
        createdAt = 10,
        updatedAt = 20,
        content = sampleResumeContent(),
        sourceResumeId = 11,
        keywords = listOf("Kotlin"),
    )

    @Test
    fun opensSavedDraftAndBackDiscardsChangesWithoutWriting() = runOnUiThread {
        lifecycle.resume()
        val original = original()
        dependencies.resumesRepository.records.value = listOf(original)
        val component = DefaultResumesComponent(appComponentContext(), 7, back = {})
        component.onEdit(original.copy(projectId = 99))
        component.onEdit(original.copy(id = 0))
        assertNull(component.editor)
        component.onEdit(original)
        val editor = assertIs<DefaultResumeEditorComponent>(component.editor)
        assertEquals(original, editor.initialResume)
        assertEquals(original, editor.draft.value.applyTo(original))
        component.onAdd()
        component.onEdit(original.copy(name = "Other"))
        assertSame(editor, component.editor)
        editor.onDraftChange(editor.draft.value.copy(name = "Changed"))
        (component.backHandler as BackDispatcher).back()
        assertNull(component.editor)
        assertEquals(listOf(original), dependencies.resumesRepository.records.value)
        component.onEdit(original)
        assertEquals("Mobile", component.editor?.draft?.value?.name)
    }

    @Test
    fun failedUpdateRetainsDraftAndRetryUpdatesWithoutCreatingAnotherResume() = runBlocking<Unit> {
        val original = original()
        dependencies.resumesRepository.records.value = listOf(original)
        val failure = IllegalStateException("Disk full")
        var fail = true
        var writes = 0
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repository = object : ResumesRepository by dependencies.resumesRepository {
            override suspend fun update(record: Resume) {
                writes++
                if (fail) throw failure
                entered.complete(Unit)
                release.await()
                dependencies.resumesRepository.update(record)
            }
        }
        val component = component(repository)
        val editor = openEditor(component, original)
        val changed = editor.draft.value.copy(name = "Backend", keywords = "JVM, Kotlin")
        runOnUiThread {
            editor.onDraftChange(changed)
            editor.onSave()
        }
        withTimeout(10_000) {
            val logged = dependencies.logger.failures.receive()
            assertSame(failure, logged.error)
            assertEquals("Update resume", logged.operation)
            assertEquals(mapOf("projectId" to 7L, "resumeId" to 12L), logged.context)
            runOnUiThread {
                assertEquals(ResumeSaveState.Failed, editor.saveState.value)
                assertEquals(changed, editor.draft.value)
                assertEquals(listOf(original), dependencies.resumesRepository.records.value)
                fail = false
                editor.onSave()
                editor.onSave()
                component.onBack()
                assertSame(editor, component.editor)
            }
            entered.await()
            assertEquals(2, writes)
            finishUpdate(component, release)
            assertEquals(
                listOf(changed.applyTo(original)),
                dependencies.resumesRepository.records.value,
            )
        }
    }

    @Test
    fun destructionCancelsPendingUpdateWithoutLoggingAnError() = runBlocking<Unit> {
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val repository = object : ResumesRepository by dependencies.resumesRepository {
            override suspend fun update(record: Resume) {
                entered.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
        }
        val component = component(repository)
        val editor = openEditor(component, original())
        runOnUiThread { editor.onSave() }
        withTimeout(10_000) {
            entered.await()
            runOnUiThread { lifecycle.destroy() }
            cancelled.await()
            assertTrue(dependencies.logger.failures.tryReceive().isFailure)
        }
    }

    private fun component(repository: ResumesRepository) = runOnUiThread {
        lifecycle.resume()
        DefaultResumesComponent(
            DefaultAppComponentContext(
                DefaultComponentContext(lifecycle),
                object : ApplicationDependencies by dependencies {
                    override val resumesRepository = repository
                },
            ),
            7,
            back = {},
        )
    }

    private fun openEditor(component: DefaultResumesComponent, original: Resume) = runOnUiThread {
        component.onEdit(original)
        assertIs<DefaultResumeEditorComponent>(component.editor)
    }

    private suspend fun finishUpdate(
        component: DefaultResumesComponent,
        release: CompletableDeferred<Unit>,
    ) {
        val finished = CompletableDeferred<Unit>()
        runOnUiThread {
            component.childStack.subscribe { if (component.editor == null) finished.complete(Unit) }
        }
        release.complete(Unit)
        finished.await()
        runOnUiThread { assertNull(component.editor) }
    }
}
