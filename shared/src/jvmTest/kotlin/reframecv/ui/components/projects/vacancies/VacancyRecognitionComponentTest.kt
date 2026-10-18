package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.recognition.RecognitionDocument
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionFileException
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.RecognizedVacancy
import reframecv.recognition.VacancyDocumentReader
import reframecv.recognition.VacancyRecognitionRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.common.ai.AiRequestDialogComponent
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancyRecognitionState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.RecognizeVacancyUseCase
import reframecv.usecase.VacancyFilesUseCases

class VacancyRecognitionComponentTest : ComponentTest() {
    private val result = RecognizedVacancy(
        jobTitle = "Mobile developer",
        description = "Build mobile apps",
        company = "Company",
        url = "https://example.com/job",
        keywords = listOf("Kotlin"),
        responsibilities = listOf("Build apps"),
        requirements = listOf("Kotlin"),
        niceToHave = listOf("CI/CD"),
    )
    private val selection = SelectedAttachment("original-file", "job.pdf")

    @Test
    fun opensDialogWithoutRequestAndAppliesOnlySuccessfulResultWithoutSaving() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        var saves = 0
        val editor = editor(
            repository { connection, instruction ->
                assertEquals("model", connection.modelId)
                assertEquals("Edited request", instruction)
                calls++
                entered.complete(Unit)
                release.await()
                RecognitionOutcome.Recognized(result)
            },
            save = { saves++ },
        )
        attach(editor)
        runOnUiThread {
            editor.onDraftChange(VacancyDraft("My shortlist", jobTitle = "Old"))
            editor.onRecognize()
        }
        val dialog = readyDialog(editor)
        assertEquals(0, calls)
        runOnUiThread {
            assertEquals("job.pdf", dialog.state.value.sourceName)
            dialog.onText("Edited request")
            dialog.onRun()
        }
        withTimeout(10_000) { entered.await() }
        runOnUiThread {
            dialog.onRun()
            editor.onSave(VacancyDraft("Blocked"))
            editor.onDraftChange(VacancyDraft("Blocked"))
            editor.snapshot.onRemove()
            assertEquals("My shortlist", editor.draft.value.name)
            assertEquals(selection, editor.snapshot.state.value.selected)
        }
        release.complete(Unit)
        awaitState(editor) { it == VacancyRecognitionState.Applied }
        runOnUiThread {
            assertNull(editor.recognition.dialog.value.component)
            assertEquals("My shortlist", editor.draft.value.name)
            assertEquals("Mobile developer", editor.draft.value.jobTitle)
            assertEquals("Company", editor.draft.value.company)
            assertEquals(SnapshotChange.Attach(selection), editor.snapshot.change)
            assertEquals(0, saves)
            editor.onSave(editor.draft.value)
            assertEquals(1, saves)
            assertEquals(1, calls)
        }
        assertEquals("Extract", dependencies.promptsRepository.records.value.single().text)
    }

    @Test
    fun rejectionStaysInDialogWithDraftAndEditedPromptAndCanBeRetried() = runBlocking {
        var calls = 0
        val editor = editor(
            repository { _, _ ->
                if (++calls == 1) {
                    RecognitionOutcome.Failed(RecognitionError.NotRecognized, "Not a job")
                } else {
                    RecognitionOutcome.Recognized(result)
                }
            },
        )
        attach(editor)
        runOnUiThread {
            editor.onDraftChange(VacancyDraft("Keep", description = "Original"))
            editor.onRecognize()
        }
        val dialog = readyDialog(editor)
        runOnUiThread {
            dialog.onText("My prompt")
            dialog.onRun()
        }
        awaitDialog(dialog) { !it.running && it.error != null }
        runOnUiThread {
            assertSame(dialog, editor.recognition.dialog.value.component)
            assertEquals("Not a job", dialog.state.value.error)
            assertEquals("My prompt", dialog.state.value.text)
            assertEquals("Original", editor.draft.value.description)
            dialog.onRun()
        }
        awaitState(editor) { it == VacancyRecognitionState.Applied }
        assertTrue(dependencies.vacanciesRepository.records.value.isEmpty())
    }

    @Test
    fun fileFailureIsShownInDialogWithoutCallingTheProvider() = runBlocking {
        var requests = 0
        val editor = editor(
            repository { _, _ ->
                requests++
                RecognitionOutcome.Recognized(result)
            },
            reader = VacancyDocumentReader {
                throw RecognitionFileException(RecognitionError.InvalidPdf)
            },
        )
        attach(editor)
        runOnUiThread { editor.onRecognize() }
        val dialog = readyDialog(editor)
        runOnUiThread { dialog.onRun() }
        awaitDialog(dialog) { !it.running && it.error != null }
        assertEquals(0, requests)
        assertEquals("The source is not a valid PDF.", dialog.state.value.error)
        assertNotNull(editor.recognition.dialog.value.component)
        Unit
    }

    @Test
    fun backCancelsDialogAndDiscardsLateResultsWithoutClosingVacancy() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        var closes = 0
        val editor = editor(
            repository { _, _ ->
                withContext(NonCancellable) {
                    entered.complete(Unit)
                    release.await()
                    returned.complete(Unit)
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
        val dialog = readyDialog(editor)
        runOnUiThread { dialog.onRun() }
        withTimeout(10_000) { entered.await() }
        runOnUiThread {
            editor.onClose()
            assertNull(editor.recognition.dialog.value.component)
            assertEquals(0, closes)
            editor.onDraftChange(VacancyDraft("After cancellation"))
        }
        release.complete(Unit)
        withTimeout(10_000) { returned.await() }
        runOnUiThread {
            assertEquals("After cancellation", editor.draft.value.name)
            assertEquals("", editor.draft.value.jobTitle)
        }
    }

    @Test
    fun disposalCancelsPreparationAndDisablingDuringPreparationPreventsSending() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var requests = 0
        val editor =
            editor(
                repository { _, _ ->
                    requests++
                    RecognitionOutcome.Recognized(result)
                },
                reader = VacancyDocumentReader {
                    entered.complete(Unit)
                    release.await()
                    RecognitionDocument("job.pdf", byteArrayOf())
                },
            )
        attach(editor)
        runOnUiThread { editor.onRecognize() }
        val dialog = readyDialog(editor)
        runOnUiThread { dialog.onRun() }
        withTimeout(10_000) { entered.await() }
        dependencies.providerSettingsUseCases.setActive(1, false)
        release.complete(Unit)
        awaitDialog(dialog) { !it.running && it.error != null }
        assertEquals(0, requests)
        runOnUiThread { lifecycle.destroy() }
    }

    @Test
    fun destroyingTheEditorCancelsFilePreparationBeforeAnyRemoteRequest() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        var requests = 0
        val editor = editor(
            repository { _, _ ->
                requests++
                RecognitionOutcome.Recognized(result)
            },
            reader = VacancyDocumentReader {
                entered.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            },
        )
        attach(editor)
        runOnUiThread { editor.onRecognize() }
        val dialog = readyDialog(editor)
        runOnUiThread { dialog.onRun() }
        withTimeout(10_000) { entered.await() }
        runOnUiThread { lifecycle.destroy() }
        withTimeout(10_000) { cancelled.await() }
        assertEquals(0, requests)
    }

    private fun repository(action: suspend (ProviderConnection, String) -> RecognitionOutcome) =
        object : VacancyRecognitionRepository {
            override suspend fun recognize(
                document: RecognitionDocument,
                connection: ProviderConnection,
                instruction: String,
            ) = action(connection, instruction)
        }

    private suspend fun readyDialog(
        editor: DefaultVacancyEditorComponent,
    ): AiRequestDialogComponent {
        val dialog = runOnUiThread { checkNotNull(editor.recognition.dialog.value.component) }
        awaitDialog(dialog) { !it.loading }
        return dialog
    }

    private suspend fun awaitDialog(
        dialog: AiRequestDialogComponent,
        predicate: (reframecv.ui.components.common.ai.AiRequestDialogState) -> Boolean,
    ) {
        val reached = CompletableDeferred<Unit>()
        runOnUiThread { dialog.state.subscribe { if (predicate(it)) reached.complete(Unit) } }
        withTimeout(10_000) { reached.await() }
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

    private suspend fun editor(
        repository: VacancyRecognitionRepository,
        reader: VacancyDocumentReader = dependencies.vacancyDocumentReader,
        save: (VacancyDraft) -> Unit = {},
        close: () -> Unit = {},
    ): DefaultVacancyEditorComponent {
        dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Test",
                apiKey = "test",
                modelId = "model",
            ),
        )
        dependencies.promptsUseCases.save(
            Prompt(text = "Extract", component = PromptComponent.Vacancies),
        )
        return runOnUiThread {
            lifecycle.resume()
            val services = object : ApplicationDependencies by dependencies {
                override val vacancyRecognitionRepository = repository
                override val vacancyDocumentReader = reader
                override val attachmentPicker = AttachmentPicker { selection }
                override val recognizeVacancy get() = RecognizeVacancyUseCase(
                    vacancyDocumentReader,
                    vacancyRecognitionRepository,
                    providerSettingsUseCases,
                )
                override val vacancyFilesUseCases get() = VacancyFilesUseCases(
                    vacancyImporter,
                    vacancyExporter,
                    attachmentPicker,
                )
            }
            DefaultVacancyEditorComponent(
                DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
                save,
                close,
            )
        }
    }
}
