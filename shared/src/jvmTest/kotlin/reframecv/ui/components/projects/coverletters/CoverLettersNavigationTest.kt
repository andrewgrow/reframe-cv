package reframecv.ui.components.projects.coverletters

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.resumes.ResumesComponent
import reframecv.ui.components.projects.resumes.editor.ResumeEditorComponent
import reframecv.ui.threading.runOnUiThread

class CoverLettersNavigationTest : ComponentTest() {
    @Test
    fun opensSourceResumeAndBackRetainsSelectedLettersThenLetterEditorReturnsToList() =
        runBlocking<Unit> {
            val letter = CoverLetter(2, 7, "Letter", 0, 0, sourceResumeId = 1)
            dependencies.coverLettersRepository.records.value = listOf(letter)
            dependencies.resumesRepository.records.value = listOf(Resume(1, 7, "Main", 0, 0))
            val ready = CompletableDeferred<Unit>()
            val opened = CompletableDeferred<ResumeEditorComponent>()
            val component = runOnUiThread {
                lifecycle.resume()
                DefaultCoverLettersComponent(appComponentContext(), 7, back = {
                    error("Must return to letter list")
                }).also { host ->
                    host.uiState.subscribe {
                        if (it is CoverLettersState.Ready &&
                            it.linkedResumes.isNotEmpty()
                        ) {
                            ready.complete(Unit)
                        }
                    }
                    host.childStack.subscribe { stack ->
                        val source =
                            stack.active.instance as? CoverLettersComponent.Child.LinkedResume
                        source?.component?.childStack?.subscribe {
                            val page = it.active.instance as? ResumesComponent.Child.Editor
                            page?.component?.let { editor -> opened.complete(editor) }
                        }
                    }
                }
            }
            withTimeout(5000) { ready.await() }
            runOnUiThread {
                component.selection.onToggle(2)
                component.onOpenResume(999)
                assertIs<CoverLettersComponent.Child.List>(
                    component.childStack.value.active.instance,
                )
                component.onOpenResume(1)
            }
            val editor = withTimeout(5000) { opened.await() }
            runOnUiThread {
                assertEquals(1L, editor.initialResume!!.id)
                component.onBack()
                assertIs<CoverLettersComponent.Child.List>(
                    component.childStack.value.active.instance,
                )
                assertEquals(setOf(2L), component.selection.state.value.selected)
                component.onEdit(letter)
                val page =
                    assertIs<CoverLettersComponent.Child.Editor>(
                        component.childStack.value.active.instance,
                    )
                assertEquals(2L, page.component.state.value.record.id)
                component.onBack()
                assertIs<CoverLettersComponent.Child.List>(
                    component.childStack.value.active.instance,
                )
            }
        }

    @Test
    fun liveSourceNamesAndMissingSourcesAreReflectedWithoutChangingIdOrder() = runBlocking<Unit> {
        dependencies.coverLettersRepository.records.value = listOf(
            CoverLetter(3, 7, "A", 0, 0),
            CoverLetter(1, 7, "Z", 0, 0, sourceResumeId = 5),
            CoverLetter(2, 9, "Other project", 0, 0),
        )
        val source = Resume(5, 7, "Original", 0, 0)
        dependencies.resumesRepository.records.value = listOf(source)
        val updates = Channel<CoverLettersState.Ready>(Channel.UNLIMITED)
        runOnUiThread {
            lifecycle.resume()
            DefaultCoverLettersComponent(appComponentContext(), 7, back = {}).uiState.subscribe {
                if (it is CoverLettersState.Ready) updates.trySend(it)
            }
        }
        val first = withTimeout(5000) { updates.receive() }
        assertEquals(listOf(1L, 3L), first.letters.map { it.id })
        assertEquals("Original", first.linkedResumes.single().name)
        dependencies.resumesRepository.records.value = listOf(source.copy(name = "Renamed"))
        val renamed = withTimeout(5000) {
            var update = updates.receive()
            while (update.linkedResumes.singleOrNull()?.name != "Renamed") {
                update = updates.receive()
            }
            update
        }
        assertEquals("Renamed", renamed.linkedResumes.single().name)
        dependencies.resumesRepository.records.value = emptyList()
        val removed = withTimeout(5000) {
            var update = updates.receive()
            while (update.linkedResumes.isNotEmpty()) update = updates.receive()
            update
        }
        assertEquals(emptyList(), removed.linkedResumes)
        updates.close()
    }
}
