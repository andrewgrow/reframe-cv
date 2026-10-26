package reframecv.ui.components.projects.vacancies

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
import reframecv.domain.models.vacancy.Vacancy
import reframecv.export.DocumentExportResult
import reframecv.export.VacancyExporter
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyExportComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyExportFormat
import reframecv.ui.components.projects.vacancies.editor.VacancyExportState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.VacancyFilesUseCases

class VacancyExportComponentTest : ComponentTest() {
    private val record =
        Vacancy(id = 12, projectId = 7, name = "Mobile", createdAt = 0, updatedAt = 0)

    @Test
    fun downloadRemainsBusyUntilFileHandlingCompletesAndIgnoresDuplicateRequests() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        var calls = 0
        val component = component(
            VacancyExporter {
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
                assertEquals(VacancyExportState.Exporting, component.state.value)
                component.onDownload()
                component.onReset()
                component.onFormatChange(VacancyExportFormat.Pdf)
                assertEquals(VacancyExportFormat.Json, component.format.value)
                assertEquals(VacancyExportState.Exporting, component.state.value)
                assertFalse(finished.isCompleted)
            }
            assertEquals(1, calls)
            release.complete(Unit)
            finished.await()
            runOnUiThread { assertEquals(VacancyExportState.Idle, component.state.value) }
        }
    }

    @Test
    fun failuresAreLoggedAndCancellationOnRetryFinishesNormally() = runBlocking {
        val failure = IllegalStateException("Could not write file")
        var fail = true
        val finished = CompletableDeferred<Unit>()
        val component = component(
            VacancyExporter {
                if (fail) throw failure
                DocumentExportResult.Cancelled
            },
        ) { finished.complete(Unit) }
        runOnUiThread { component.onDownload() }
        withTimeout(10_000) {
            val logged = dependencies.logger.failures.receive()
            assertSame(failure, logged.error)
            assertEquals("Export vacancy as JSON", logged.operation)
            assertEquals(mapOf("projectId" to 7L, "vacancyId" to 12L), logged.context)
            runOnUiThread {
                assertEquals(VacancyExportState.Failed, component.state.value)
                assertFalse(finished.isCompleted)
                fail = false
                component.onDownload()
            }
            finished.await()
            runOnUiThread { assertEquals(VacancyExportState.Idle, component.state.value) }
        }
    }

    @Test
    fun pdfFailuresRemainRetryableAndResetReturnsToTheDefaultPdfFormat() = runBlocking {
        val failure = IllegalStateException("PDF rendering failed")
        val component = component(VacancyExporter { throw failure }, VacancyExportFormat.Pdf) {}
        runOnUiThread { component.onDownload() }
        withTimeout(5000) {
            val logged = dependencies.logger.failures.receive()
            assertEquals("Export vacancy as PDF", logged.operation)
            assertSame(failure, logged.error)
            runOnUiThread {
                assertEquals(VacancyExportState.Failed, component.state.value)
                component.onFormatChange(VacancyExportFormat.Json)
                assertEquals(VacancyExportState.Idle, component.state.value)
                component.onReset()
                assertEquals(VacancyExportFormat.Pdf, component.format.value)
            }
        }
    }

    private fun component(
        exporter: VacancyExporter,
        format: VacancyExportFormat = VacancyExportFormat.Json,
        finished: () -> Unit,
    ) = runOnUiThread {
        lifecycle.resume()
        DefaultVacancyExportComponent(
            DefaultAppComponentContext(
                DefaultComponentContext(lifecycle),
                object : ApplicationDependencies by dependencies {
                    override val vacancyFilesUseCases get() = VacancyFilesUseCases(
                        vacancyImporter,
                        vacancyExporter,
                        attachmentPicker,
                        vacancyPdfExporter,
                    )
                    override val vacancyExporter = exporter
                    override val vacancyPdfExporter = exporter
                },
            ),
            vacancy = { record },
            finished = finished,
        ).also { it.onFormatChange(format) }
    }
}
