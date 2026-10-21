package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.export.CoverLetterExporter
import reframecv.export.DocumentExportResult
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.coverletters.CoverLetterExportState
import reframecv.ui.components.projects.coverletters.DefaultCoverLetterExportComponent
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.CoverLetterFilesUseCases

class CoverLetterExportComponentTest : ComponentTest() {
    private val record =
        CoverLetter(id = 12, projectId = 7, name = "Mobile", createdAt = 0, updatedAt = 0)

    @Test
    fun downloadRemainsBusyUntilFileHandlingCompletesAndIgnoresDuplicateRequests() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        var calls = 0
        val component = component(
            CoverLetterExporter {
                assertEquals(record, it)
                calls++
                entered.complete(Unit)
                release.await()
                DocumentExportResult.Saved
            },
        ) { finished.complete(Unit) }
        runOnUiThread { component.onDownload() }
        withTimeout(10_000) {
            entered.await()
            runOnUiThread {
                assertEquals(CoverLetterExportState.Exporting, component.state.value)
                component.onDownload()
                component.onReset()
                assertEquals(CoverLetterExportState.Exporting, component.state.value)
                assertFalse(finished.isCompleted)
            }
            assertEquals(1, calls)
            release.complete(Unit)
            finished.await()
            runOnUiThread { assertEquals(CoverLetterExportState.Idle, component.state.value) }
        }
    }

    @Test
    fun failuresAreLoggedAndCancellationOnRetryFinishesNormally() = runBlocking {
        val failure = IllegalStateException("Could not write file")
        var fail = true
        val finished = CompletableDeferred<Unit>()
        val component = component(
            CoverLetterExporter {
                if (fail) throw failure
                DocumentExportResult.Cancelled
            },
        ) { finished.complete(Unit) }
        runOnUiThread { component.onDownload() }
        withTimeout(10_000) {
            val logged = dependencies.logger.failures.receive()
            assertSame(failure, logged.error)
            assertEquals("Export cover letter as JSON", logged.operation)
            assertEquals(mapOf("projectId" to 7L, "coverLetterId" to 12L), logged.context)
            runOnUiThread {
                assertEquals(CoverLetterExportState.Failed, component.state.value)
                assertFalse(finished.isCompleted)
                fail = false
                component.onDownload()
            }
            finished.await()
            runOnUiThread { assertEquals(CoverLetterExportState.Idle, component.state.value) }
        }
    }

    private fun component(exporter: CoverLetterExporter, finished: () -> Unit) = runOnUiThread {
        lifecycle.resume()
        DefaultCoverLetterExportComponent(
            DefaultAppComponentContext(
                DefaultComponentContext(lifecycle),
                object : ApplicationDependencies by dependencies {
                    override val coverLetterFilesUseCases get() = CoverLetterFilesUseCases(
                        coverLetterImporter,
                        coverLetterExporter,
                    )
                    override val coverLetterExporter = exporter
                },
            ),
            coverLetter = { record },
            finished = finished,
        )
    }
}
