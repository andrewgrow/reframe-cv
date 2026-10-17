package reframecv.ui.components.projects.resumes

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
import reframecv.domain.models.resume.Resume
import reframecv.export.ResumeExportResult
import reframecv.export.ResumeExporter
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.resumes.editor.DefaultResumeExportComponent
import reframecv.ui.components.projects.resumes.editor.ResumeExportState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ResumeFilesUseCases

class ResumeExportComponentTest : ComponentTest() {
    private val record =
        Resume(id = 12, projectId = 7, name = "Mobile", createdAt = 0, updatedAt = 0)

    @Test
    fun downloadRemainsBusyUntilFileHandlingCompletesAndIgnoresDuplicateRequests() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        var calls = 0
        val component = component(
            ResumeExporter {
                assertEquals(record, it)
                calls++
                entered.complete(Unit)
                release.await()
                ResumeExportResult.Saved
            },
        ) { finished.complete(Unit) }
        runOnUiThread { component.onDownload() }
        withTimeout(10_000) {
            entered.await()
            runOnUiThread {
                assertEquals(ResumeExportState.Exporting, component.state.value)
                component.onDownload()
                component.onReset()
                assertEquals(ResumeExportState.Exporting, component.state.value)
                assertFalse(finished.isCompleted)
            }
            assertEquals(1, calls)
            release.complete(Unit)
            finished.await()
            runOnUiThread { assertEquals(ResumeExportState.Idle, component.state.value) }
        }
    }

    @Test
    fun failuresAreLoggedAndCancellationOnRetryFinishesNormally() = runBlocking {
        val failure = IllegalStateException("Could not write file")
        var fail = true
        val finished = CompletableDeferred<Unit>()
        val component = component(
            ResumeExporter {
                if (fail) throw failure
                ResumeExportResult.Cancelled
            },
        ) { finished.complete(Unit) }
        runOnUiThread { component.onDownload() }
        withTimeout(10_000) {
            val logged = dependencies.logger.failures.receive()
            assertSame(failure, logged.error)
            assertEquals("Export resume as JSON", logged.operation)
            assertEquals(mapOf("projectId" to 7L, "resumeId" to 12L), logged.context)
            runOnUiThread {
                assertEquals(ResumeExportState.Failed, component.state.value)
                assertFalse(finished.isCompleted)
                fail = false
                component.onDownload()
            }
            finished.await()
            runOnUiThread { assertEquals(ResumeExportState.Idle, component.state.value) }
        }
    }

    private fun component(exporter: ResumeExporter, finished: () -> Unit) = runOnUiThread {
        lifecycle.resume()
        DefaultResumeExportComponent(
            DefaultAppComponentContext(
                DefaultComponentContext(lifecycle),
                object : ApplicationDependencies by dependencies {
                    override val resumeFilesUseCases get() = ResumeFilesUseCases(
                        resumeImporter,
                        resumeExporter,
                        resumePhotos,
                    )
                    override val resumeExporter = exporter
                },
            ),
            resume = { record },
            finished = finished,
        )
    }
}
