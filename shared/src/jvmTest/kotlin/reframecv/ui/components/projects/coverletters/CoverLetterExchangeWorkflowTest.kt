package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.export.CoverLetterExporter
import reframecv.export.DocumentExportResult
import reframecv.importing.CoverLetterImporter
import reframecv.importing.ImportedCoverLetterFile
import reframecv.repository.CoverLettersRepository
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.CoverLetterFilesUseCases
import reframecv.usecase.CoverLettersUseCases

class CoverLetterExchangeWorkflowTest : ComponentTest() {
    private val record = CoverLetter(1, 7, "Original", 0, 0, content = "Old", sourceResumeId = 9)

    @Test
    fun importFillsOnlyDraftSaveCreatesOnceAndClosingReleasesTemporarySource() = runBlocking<Unit> {
        val release = CompletableDeferred<Unit>()
        val deleted = CompletableDeferred<Unit>()
        var calls = 0
        var closes = 0
        val component =
            editor(
                record.copy(id = 0, sourceResumeId = null),
                importer = CoverLetterImporter {
                    calls++
                    release.await()
                    object : ImportedCoverLetterFile {
                        override val coverLetter = record.copy(
                            id = 99,
                            projectId = 99,
                            name = "Imported",
                            content = "New",
                        )
                        override suspend fun deleteTemporaryFile() {
                            deleted.complete(Unit)
                        }
                    }
                },
                close = { closes++ },
            )
        val states = Channel<CoverLetterEditorState>(Channel.UNLIMITED)
        runOnUiThread {
            component.state.subscribe { states.trySend(it) }
            component.onImport()
            assertTrue(component.state.value.busy)
            component.onSave()
            component.onImport()
            component.onClose()
            assertEquals(0, closes)
            assertTrue(dependencies.coverLettersRepository.records.value.isEmpty())
        }
        release.complete(Unit)
        withTimeout(5000) {
            while (states.receive().record.name != "Imported") { /* Await import. */ }
            runOnUiThread {
                assertEquals(0L, component.state.value.record.id)
                assertEquals(7L, component.state.value.record.projectId)
                assertEquals(null, component.state.value.record.sourceResumeId)
                assertTrue(dependencies.coverLettersRepository.records.value.isEmpty())
                component.onSave()
            }
            while (states.receive().notice != LetterNotice.Saved) { /* Await Save. */ }
            runOnUiThread {
                assertTrue(component.state.value.record.id > 0)
                component.onImport()
                component.onChange(component.state.value.record.copy(content = "Updated"))
                component.onSave()
            }
            while (states.receive().record.content != "Updated") { /* Await draft. */ }
            while (states.receive().notice != LetterNotice.Saved) { /* Await update. */ }
            runOnUiThread { component.onClose() }
            deleted.await()
        }
        assertEquals(1, calls)
        assertEquals(1, closes)
        assertEquals("Updated", dependencies.coverLettersRepository.records.value.single().content)
        states.close()
    }

    @Test
    fun exportSavesFirstAndBlocksClosingWhileDownloadingThenReturnsToEditor() = runBlocking<Unit> {
        dependencies.coverLettersRepository.records.value = listOf(record)
        val entered = CompletableDeferred<CoverLetter>()
        val release = CompletableDeferred<Unit>()
        var closes = 0
        val component = editor(
            record,
            exporter = CoverLetterExporter {
                entered.complete(it)
                release.await()
                DocumentExportResult.Cancelled
            },
            close = { closes++ },
        )
        val opened = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        runOnUiThread {
            component.state.subscribe {
                if (it.exportVisible && !it.busy) opened.complete(Unit)
                if (entered.isCompleted && !it.exportVisible) finished.complete(Unit)
            }
            component.onChange(record.copy(content = "Current draft"))
            component.onExport()
        }
        withTimeout(5000) { opened.await() }
        runOnUiThread {
            assertEquals(
                "Current draft",
                dependencies.coverLettersRepository.records.value.single().content,
            )
            component.exportComponent.onDownload()
            assertTrue(component.state.value.busy)
            component.onClose()
            component.onCloseExport()
            assertTrue(component.state.value.exportVisible)
        }
        assertEquals("Current draft", withTimeout(5000) { entered.await() }.content)
        release.complete(Unit)
        withTimeout(5000) { finished.await() }
        assertEquals(0, closes)
    }

    @Test
    fun failedSaveDoesNotOpenExportAndKeepsDraftForRetry() = runBlocking<Unit> {
        val repository = object : CoverLettersRepository by dependencies.coverLettersRepository {
            override suspend fun update(record: CoverLetter) {
                error("Database unavailable")
            }
        }
        val component = editor(record, repository = repository)
        runOnUiThread {
            component.onChange(record.copy(content = "Draft"))
            component.onExport()
        }
        withTimeout(5000) { dependencies.logger.failures.receive() }
        runOnUiThread {
            assertEquals(LetterNotice.SaveFailed, component.state.value.notice)
            assertFalse(component.state.value.exportVisible)
            assertEquals("Draft", component.state.value.record.content)
            component.onChange(record.copy(name = " "))
            component.onExport()
            assertEquals(LetterNotice.Validation, component.state.value.notice)
        }
    }
    private fun editor(
        initial: CoverLetter,
        importer: CoverLetterImporter = CoverLetterImporter { null },
        exporter: CoverLetterExporter = CoverLetterExporter { DocumentExportResult.Cancelled },
        repository: CoverLettersRepository = dependencies.coverLettersRepository,
        close: () -> Unit = {},
    ) = runOnUiThread {
        lifecycle.resume()
        val services = object : ApplicationDependencies by dependencies {
            override val coverLetterFilesUseCases = CoverLetterFilesUseCases(importer, exporter)
            override val coverLettersUseCases = CoverLettersUseCases(repository)
        }
        DefaultCoverLetterEditorComponent(
            DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
            initial,
            close,
            {
            },
        )
    }
}
