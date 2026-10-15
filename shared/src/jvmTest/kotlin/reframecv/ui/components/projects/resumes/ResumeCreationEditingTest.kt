package reframecv.ui.components.projects.resumes

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.resumes.editor.DefaultResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeRole
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.threading.runOnUiThread

class ResumeCreationEditingTest : ComponentTest() {
    @Test
    fun firstSaveKeepsEditorAndSubsequentSaveExportAndDeleteUseAssignedId() = runBlocking {
        val saves = Channel<ResumeSaveState>(Channel.UNLIMITED)
        val component = runOnUiThread {
            lifecycle.resume()
            DefaultResumesComponent(appComponentContext(), 7, back = {}).also { it.onAdd() }
        }
        val editor = runOnUiThread {
            (checkNotNull(component.editor) as DefaultResumeEditorComponent).also {
                it.saveState.subscribe(saves::trySend)
                it.onDraftChange(ResumeDraft("First", role = ResumeRole.Main, photoVisible = false))
                it.onSave()
            }
        }
        withTimeout(10_000) {
            saves.awaitSaved()
            val created = dependencies.resumesRepository.records.value.single()
            runOnUiThread {
                assertSame(editor, component.editor)
                assertEquals(2, component.childStack.value.items.size)
                assertEquals(created, editor.initialResume)
                assertEquals(1L, editor.successfulSaves.value)
                assertFalse(editor.draft.value.photoVisible)
                editor.onDraftChange(
                    editor.draft.value.copy(name = "Updated", role = ResumeRole.Regular),
                )
                editor.onSave()
            }
            saves.awaitSaved()
            val updated = dependencies.resumesRepository.records.value.single()
            assertEquals(created.id, updated.id)
            assertEquals("Updated", updated.name)
            assertEquals(null, dependencies.resumesRepository.mainResumeIds.value[7])
            runOnUiThread { editor.onExport() }
            saves.awaitSaved()
            runOnUiThread {
                assertSame(editor, component.editor)
                assertTrue(editor.exportVisible.value)
                assertNotNull(editor.initialResume)
                editor.onCloseExport()
                editor.onRequestDelete()
                editor.onDeleteConfirmationChange("delete")
                editor.onConfirmDelete()
            }
            val closed = Channel<Unit>(Channel.CONFLATED)
            runOnUiThread {
                component.childStack.subscribe {
                    if (component.editor ==
                        null
                    ) {
                        closed.trySend(Unit)
                    }
                }
            }
            closed.receive()
            assertTrue(dependencies.resumesRepository.records.value.isEmpty())
        }
    }

    private suspend fun Channel<ResumeSaveState>.awaitSaved() {
        while (receive() != ResumeSaveState.Saved) { /* Wait for the completed write. */ }
    }
}
