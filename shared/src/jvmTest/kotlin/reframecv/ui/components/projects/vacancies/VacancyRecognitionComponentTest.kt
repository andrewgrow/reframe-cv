package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import reframecv.attachments.AttachmentPicker
import reframecv.attachments.SelectedAttachment
import reframecv.attachments.SnapshotChange
import reframecv.dependencies.ApplicationDependencies
import reframecv.recognition.RecognitionDocument
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionFileException
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.VacancyDocumentReader
import reframecv.recognition.VacancyRecognitionJson
import reframecv.recognition.VacancyRecognitionRepository
import reframecv.recognition.recognitionFixture
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancyRecognitionState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class VacancyRecognitionComponentTest : ComponentTest() {
    private val result = VacancyRecognitionJson.decode(recognitionFixture())
    private val selection = SelectedAttachment("original-file", "job.pdf")

    @Test
    fun appliesPostingFieldsOnlyAfterSuccessAndKeepsNameAndAttachmentUntilExplicitSave() =
        runBlocking {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var calls = 0
            var saves = 0
            val editor = editor(
                repository {
                    calls++
                    entered.complete(Unit)
                    release.await()
                    RecognitionOutcome.Recognized(result)
                },
                save = { saves++ },
            )
            attach(editor)
            runOnUiThread {
                editor.onDraftChange(
                    VacancyDraft("My shortlist", company = "Old", jobTitle = "Old title"),
                )
                editor.onRecognize()
            }
            withTimeout(10_000) { entered.await() }
            runOnUiThread {
                assertTrue(editor.isBusy)
                editor.onRecognize()
                editor.onImport()
                editor.onDraftChange(VacancyDraft("Blocked"))
                editor.onSave(VacancyDraft("Blocked"))
                editor.snapshot.onRemove()
                assertEquals("My shortlist", editor.draft.value.name)
                assertEquals(selection, editor.snapshot.state.value.selected)
            }
            release.complete(Unit)
            awaitState(editor) { it == VacancyRecognitionState.Applied }
            runOnUiThread {
                assertEquals("My shortlist", editor.draft.value.name)
                assertEquals("Mobile developer", editor.draft.value.jobTitle)
                assertEquals("Recognized company", editor.draft.value.company)
                assertEquals("https://example.com/recognized", editor.draft.value.url)
                assertEquals(
                    listOf("Kotlin", "Android"),
                    editor.draft.value.applyTo(editor.draft.value.toVacancy(0)).keywords,
                )
                assertEquals(SnapshotChange.Attach(selection), editor.snapshot.change)
                assertEquals(0, saves)
                editor.onSave(editor.draft.value)
                assertEquals(1, saves)
                assertEquals(1, calls)
            }
        }

    @Test
    fun failureKeepsDraftAndCanBeRetriedWithoutCreatingRecords() = runBlocking {
        var calls = 0
        val original = VacancyDraft("Keep me", description = "Original")
        val editor = editor(
            repository {
                if (++calls == 1) {
                    RecognitionOutcome.Failed(RecognitionError.NotConfigured)
                } else {
                    RecognitionOutcome.Recognized(result)
                }
            },
        )
        attach(editor)
        runOnUiThread {
            editor.onDraftChange(original)
            editor.onRecognize()
        }
        awaitState(editor) { it is VacancyRecognitionState.Failed }
        runOnUiThread {
            assertEquals(original, editor.draft.value)
            assertEquals(
                VacancyRecognitionState.Failed(RecognitionError.NotConfigured),
                editor.recognition.state.value,
            )
            editor.onRecognize()
        }
        awaitState(editor) { it == VacancyRecognitionState.Applied }
        assertEquals(emptyList(), dependencies.vacanciesRepository.records.value)
    }

    @Test
    fun fileFailureDoesNotCallTheRemoteRepository() = runBlocking {
        var requests = 0
        val editor = editor(
            repository {
                requests++
                RecognitionOutcome.Recognized(result)
            },
            reader = VacancyDocumentReader {
                throw RecognitionFileException(RecognitionError.InvalidPdf)
            },
        )
        attach(editor)
        runOnUiThread { editor.onRecognize() }
        awaitState(editor) { it is VacancyRecognitionState.Failed }
        runOnUiThread {
            assertEquals(
                VacancyRecognitionState.Failed(RecognitionError.InvalidPdf),
                editor.recognition.state.value,
            )
            assertEquals("", editor.draft.value.name)
        }
        assertEquals(0, requests)
        val logged = withTimeout(10_000) { dependencies.logger.failures.receive() }
        assertEquals("Recognize vacancy from PDF", logged.operation)
    }

    @Test
    fun cancellingViaBackDiscardsLateSuccessAndAllowsAnotherAttempt() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        var calls = 0
        var closes = 0
        val editor = editor(
            repository {
                if (++calls == 1) {
                    withContext(NonCancellable) {
                        entered.complete(Unit)
                        release.await()
                        returned.complete(Unit)
                    }
                }
                RecognitionOutcome.Recognized(result)
            },
            close = { closes++ },
        )
        attach(editor)
        runOnUiThread {
            editor.onDraftChange(VacancyDraft("Before"))
            editor.onRecognize()
        }
        withTimeout(10_000) { entered.await() }
        runOnUiThread {
            editor.onClose()
            assertEquals(VacancyRecognitionState.Idle, editor.recognition.state.value)
            assertEquals(0, closes)
            editor.onDraftChange(VacancyDraft("After cancellation"))
        }
        release.complete(Unit)
        withTimeout(10_000) { returned.await() }
        runOnUiThread {
            assertEquals("After cancellation", editor.draft.value.name)
            assertEquals("", editor.draft.value.jobTitle)
            editor.onRecognize()
        }
        awaitState(editor) { it == VacancyRecognitionState.Applied }
        runOnUiThread { assertEquals("After cancellation", editor.draft.value.name) }
    }

    @Test
    fun disposalCancelsFilePreparationBeforeAnyRemoteRequest() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val reader = VacancyDocumentReader {
            entered.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        var requests = 0
        val editor =
            editor(
                repository {
                    requests++
                    RecognitionOutcome.Recognized(result)
                },
                reader,
            )
        attach(editor)
        runOnUiThread { editor.onRecognize() }
        withTimeout(10_000) {
            entered.await()
            runOnUiThread { lifecycle.destroy() }
            cancelled.await()
        }
        assertEquals(0, requests)
    }

    @Test
    fun providerRejectionAndUnexpectedFailuresKeepTheOriginalDraft() = runBlocking {
        val failure = IOException("Connection failed")
        var calls = 0
        val editor = editor(
            repository {
                if (++calls ==
                    1
                ) {
                    RecognitionOutcome.Recognized(
                        result.copy(statusCode = 400, statusDetails = "Not a job"),
                    )
                } else {
                    throw failure
                }
            },
        )
        attach(editor)
        runOnUiThread {
            editor.onDraftChange(VacancyDraft("Keep"))
            editor.onRecognize()
        }
        awaitState(editor) { it is VacancyRecognitionState.Failed }
        runOnUiThread {
            assertEquals(
                VacancyRecognitionState.Failed(RecognitionError.NotRecognized, "Not a job"),
                editor.recognition.state.value,
            )
            assertEquals("Keep", editor.draft.value.name)
            editor.onRecognize()
        }
        awaitState(editor) { it is VacancyRecognitionState.Failed }
        assertSame(failure, withTimeout(10_000) { dependencies.logger.failures.receive() }.error)
        runOnUiThread { assertEquals("Keep", editor.draft.value.name) }
    }

    private fun repository(action: suspend (RecognitionDocument) -> RecognitionOutcome) =
        object : VacancyRecognitionRepository {
            override suspend fun recognize(document: RecognitionDocument) = action(document)
        }

    private suspend fun awaitState(
        editor: DefaultVacancyEditorComponent,
        predicate: (VacancyRecognitionState) -> Boolean,
    ) {
        val reached = CompletableDeferred<Unit>()
        runOnUiThread {
            editor.recognition.state.subscribe { if (predicate(it)) reached.complete(Unit) }
        }
        withTimeout(10_000) { reached.await() }
    }

    private suspend fun attach(editor: DefaultVacancyEditorComponent) {
        val ready = CompletableDeferred<Unit>()
        runOnUiThread {
            editor.snapshot.state.subscribe {
                if (it.selected != null &&
                    !it.loading
                ) {
                    ready.complete(Unit)
                }
            }
            editor.snapshot.onAttach()
        }
        withTimeout(10_000) { ready.await() }
    }

    private fun editor(
        repository: VacancyRecognitionRepository,
        reader: VacancyDocumentReader = dependencies.vacancyDocumentReader,
        save: (VacancyDraft) -> Unit = {},
        close: () -> Unit = {},
    ) = runOnUiThread {
        lifecycle.resume()
        val services = object : ApplicationDependencies by dependencies {
            override val vacancyRecognitionRepository = repository
            override val vacancyDocumentReader = reader
            override val attachmentPicker = AttachmentPicker { selection }
        }
        DefaultVacancyEditorComponent(
            DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
            save,
            close,
        )
    }
}
