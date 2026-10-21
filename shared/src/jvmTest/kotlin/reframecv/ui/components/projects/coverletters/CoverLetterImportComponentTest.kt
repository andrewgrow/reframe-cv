package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.importing.CoverLetterImporter
import reframecv.importing.ImportedCoverLetterFile
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.CoverLetterFilesUseCases

class CoverLetterImportComponentTest : ComponentTest() {
    @Test
    fun failureAndPickerCancellationLeaveTheDraftUntouchedAndAllowRetry() = runBlocking<Unit> {
        val failure = IllegalArgumentException("Invalid file")
        val deleted = CompletableDeferred<Unit>()
        val record = CoverLetter(0, 0, "Imported", 0, 0, content = "Letter text")
        var attempts = 0
        val importer = CoverLetterImporter {
            when (++attempts) {
                1 -> throw failure

                2 -> null

                else -> object : ImportedCoverLetterFile {
                    override val coverLetter = record
                    override suspend fun deleteTemporaryFile() {
                        deleted.complete(Unit)
                    }
                }
            }
        }
        val imported = mutableListOf<CoverLetter>()
        val states = Channel<CoverLetterImportState>(Channel.UNLIMITED)
        val component = runOnUiThread {
            lifecycle.resume()
            val services = object : ApplicationDependencies by dependencies {
                override val coverLetterFilesUseCases =
                    CoverLetterFilesUseCases(importer, coverLetterExporter)
            }
            DefaultCoverLetterImportComponent(
                DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
            ) { imported += it }.also { it.state.subscribe { value -> states.trySend(value) } }
        }
        runOnUiThread { component.onImport() }
        withTimeout(5000) {
            while (states.receive() != CoverLetterImportState.Failed) { /* Await failed read. */ }
            assertSame(failure, dependencies.logger.failures.receive().error)
            assertEquals(emptyList(), imported)
            runOnUiThread { component.onImport() }
            while (states.receive() !=
                CoverLetterImportState.Idle
            ) { /* Await picker cancellation. */ }
            assertEquals(emptyList(), imported)
            runOnUiThread { component.onImport() }
            while (states.receive() != CoverLetterImportState.Imported) { /* Await retry. */ }
            assertEquals(listOf(record), imported)
            assertFalse(deleted.isCompleted)
            runOnUiThread { component.onClose() }
            deleted.await()
        }
        states.close()
    }
}
