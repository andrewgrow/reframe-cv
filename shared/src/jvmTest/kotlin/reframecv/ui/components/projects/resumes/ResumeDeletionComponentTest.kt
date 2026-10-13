package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.resume.Resume
import reframecv.repository.ResumesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.resumes.editor.DefaultResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeDeleteState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class ResumeDeletionComponentTest : ComponentTest() {
    private fun original() =
        Resume(id = 12, projectId = 7, name = "Mobile", createdAt = 0, updatedAt = 0)

    @Test
    fun confirmationRequiresWordAndBackCancelsDialogBeforeLeavingPage() = runOnUiThread {
        lifecycle.resume()
        val component = DefaultResumesComponent(appComponentContext(), 7, back = {})
        component.onAdd()
        checkNotNull(component.editor).onRequestDelete()
        assertEquals(ResumeDeleteState.Idle, component.editor?.deleteState?.value)
        component.onBack()
        component.onEdit(original())
        val editor = assertIs<DefaultResumeEditorComponent>(component.editor)
        editor.onDraftChange(editor.draft.value.copy(name = ""))
        editor.onRequestDelete()
        editor.onDeleteConfirmationChange("DEL")
        assertFalse(editor.canConfirmDelete)
        editor.onConfirmDelete()
        assertEquals(ResumeDeleteState.Confirming, editor.deleteState.value)
        editor.onSave()
        assertFalse(editor.validationRequested.value)
        val back = component.backHandler as BackDispatcher
        back.back()
        assertSame(editor, component.editor)
        assertEquals(ResumeDeleteState.Idle, editor.deleteState.value)
        assertEquals("", editor.deleteConfirmation.value)
        back.back()
        assertNull(component.editor)
    }

    @Test
    fun failedDeletionRetainsConfirmationAndRetryDeletesExactlyOnce() = runBlocking<Unit> {
        val original = original()
        dependencies.resumesRepository.records.value = listOf(original)
        val failure = IllegalStateException("Disk busy")
        var fail = true
        var calls = 0
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repository = object : ResumesRepository by dependencies.resumesRepository {
            override suspend fun delete(id: Long) {
                calls++
                if (fail) throw failure
                entered.complete(Unit)
                release.await()
                dependencies.resumesRepository.delete(id)
            }
        }
        val component = component(repository)
        val editor = runOnUiThread {
            component.onEdit(original)
            assertIs<DefaultResumeEditorComponent>(component.editor).also {
                it.onRequestDelete()
                it.onDeleteConfirmationChange(" dElEtE ")
                it.onConfirmDelete()
            }
        }
        withTimeout(10_000) {
            val logged = dependencies.logger.failures.receive()
            assertEquals("Delete resume", logged.operation)
            assertEquals(mapOf("projectId" to 7L, "resumeId" to 12L), logged.context)
            assertSame(failure, logged.error)
            runOnUiThread {
                assertEquals(ResumeDeleteState.Failed, editor.deleteState.value)
                assertEquals(" dElEtE ", editor.deleteConfirmation.value)
                fail = false
                editor.onConfirmDelete()
                editor.onConfirmDelete()
                component.onBack()
                editor.onCancelDelete()
                assertSame(editor, component.editor)
            }
            entered.await()
            assertEquals(2, calls)
            finishDelete(component, release)
            assertEquals(emptyList(), dependencies.resumesRepository.records.value)
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

    private suspend fun finishDelete(
        component: DefaultResumesComponent,
        release: CompletableDeferred<Unit>,
    ) {
        val completed = CompletableDeferred<Unit>()
        runOnUiThread {
            component.childStack.subscribe {
                if (component.editor ==
                    null
                ) {
                    completed.complete(Unit)
                }
            }
        }
        release.complete(Unit)
        completed.await()
        runOnUiThread { assertNull(component.editor) }
    }
}
