package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.export.DocumentExportResult
import reframecv.export.VacancyArchive
import reframecv.export.VacancyArchiveExporter
import reframecv.repository.ResumesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyExportComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyExportFormat
import reframecv.ui.components.projects.vacancies.editor.VacancyExportState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.VacancyArchiveUseCases

class VacancyArchiveComponentTest : ComponentTest() {
    private val record = Vacancy(
        7,
        1,
        "Example vacancy",
        0,
        0,
        resumes = listOf(VacancyResume(1), VacancyResume(2)),
        coverLetterIds = listOf(1, 2),
    )

    @Test
    fun zipDraftDefaultsToAllLinkedDocumentsAndExportsOnlyUserChoices() = runBlocking<Unit> {
        dependencies.resumesRepository.records.value = listOf(
            Resume(1, 1, "First", 0, 0),
            Resume(2, 1, "Second", 0, 0),
        )
        dependencies.coverLettersRepository.records.value = listOf(
            CoverLetter(1, 1, "First", 0, 0),
            CoverLetter(2, 1, "Second", 0, 0),
        )
        val bundle = CompletableDeferred<VacancyArchive>()
        val release = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val component = component(
            VacancyArchiveExporter {
                bundle.complete(it)
                release.await()
                DocumentExportResult.Saved
            },
        ) { finished.complete(Unit) }
        val loaded = CompletableDeferred<Unit>()
        val subscription = runOnUiThread {
            component.archive.subscribe { if (it.ready) loaded.complete(Unit) }.also {
                component.onFormatChange(VacancyExportFormat.Zip)
            }
        }
        withTimeout(5000) { loaded.await() }
        runOnUiThread {
            assertEquals(record.name, component.archive.value.name)
            assertEquals(setOf(1L, 2L), component.archive.value.resumeIds)
            component.onResumeToggle(1)
            component.onLetterToggle(2)
            component.onArchiveName("Custom name")
            component.onDownload()
        }
        val selected = withTimeout(5000) { bundle.await() }
        assertEquals("Custom name", selected.name)
        assertEquals(listOf(2L), selected.resumes.map { it.id })
        assertEquals(listOf(1L), selected.coverLetters.map { it.id })
        runOnUiThread {
            component.onArchiveName("Ignored")
            component.onResumeToggle(2)
            component.onReset()
            assertEquals("Custom name", component.archive.value.name)
            assertEquals(VacancyExportState.Exporting, component.state.value)
        }
        release.complete(Unit)
        withTimeout(5000) { finished.await() }
        runOnUiThread {
            subscription.cancel()
            component.onReset()
            assertEquals(VacancyExportFormat.Pdf, component.format.value)
            assertFalse(component.archive.value.loaded)
        }
    }

    @Test
    fun blankNamesBlockDownloadAndResetDiscardsTheArchiveDraft() = runBlocking<Unit> {
        val component = runOnUiThread {
            lifecycle.resume()
            DefaultVacancyExportComponent(appComponentContext(), { record }, {})
        }
        val loaded = CompletableDeferred<Unit>()
        val subscription = runOnUiThread {
            component.archive.subscribe { if (it.loaded) loaded.complete(Unit) }.also {
                component.onFormatChange(VacancyExportFormat.Zip)
            }
        }
        withTimeout(5000) { loaded.await() }
        runOnUiThread {
            component.onArchiveName(" ... ")
            assertTrue(component.archive.value.invalidName)
            component.onDownload()
            assertEquals(VacancyExportState.Idle, component.state.value)
            component.onReset()
            subscription.cancel()
            assertEquals("", component.archive.value.name)
        }
    }

    @Test
    fun cancellingWhileChoicesLoadStopsTheReadWithoutRestoringAClosedDraft() = runBlocking<Unit> {
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val repository = object : ResumesRepository by dependencies.resumesRepository {
            override fun observeResumes(projectId: Long) = flow<List<Resume>> {
                entered.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
        }
        val component = runOnUiThread {
            lifecycle.resume()
            val services = object : ApplicationDependencies by dependencies {
                override val vacancyArchiveUseCases = VacancyArchiveUseCases(
                    repository,
                    coverLettersRepository,
                    VacancyArchiveExporter { error("Must not generate an archive") },
                )
            }
            DefaultVacancyExportComponent(
                DefaultAppComponentContext(
                    DefaultComponentContext(lifecycle),
                    services,
                ),
                {
                    record
                },
                {},
            )
                .also { it.onFormatChange(VacancyExportFormat.Zip) }
        }
        withTimeout(5000) { entered.await() }
        runOnUiThread { component.onReset() }
        withTimeout(5000) { cancelled.await() }
        assertEquals(VacancyExportFormat.Pdf, component.format.value)
        assertEquals("", component.archive.value.name)
        assertFalse(component.archive.value.loading)
    }

    @Test
    fun failedChoiceLoadingCanRetryWithoutLosingTheEnteredArchiveName() = runBlocking<Unit> {
        var fail = true
        val loaded = CompletableDeferred<Unit>()
        val repository = object : ResumesRepository by dependencies.resumesRepository {
            override fun observeResumes(projectId: Long) = flow<List<Resume>> {
                if (fail) error("Database unavailable")
                emit(emptyList())
            }
        }
        val component = runOnUiThread {
            lifecycle.resume()
            val services = object : ApplicationDependencies by dependencies {
                override val vacancyArchiveUseCases = VacancyArchiveUseCases(
                    repository,
                    coverLettersRepository,
                    VacancyArchiveExporter {
                        DocumentExportResult.Cancelled
                    },
                )
            }
            DefaultVacancyExportComponent(
                DefaultAppComponentContext(
                    DefaultComponentContext(lifecycle),
                    services,
                ),
                {
                    record
                },
                {},
            )
        }
        runOnUiThread { component.onFormatChange(VacancyExportFormat.Zip) }
        withTimeout(5000) { dependencies.logger.failures.receive() }
        runOnUiThread {
            assertTrue(component.archive.value.failed)
            component.onArchiveName("Keep this name")
            fail = false
            component.archive.subscribe { if (it.ready) loaded.complete(Unit) }
            component.onRetryArchive()
        }
        withTimeout(5000) { loaded.await() }
        assertEquals("Keep this name", component.archive.value.name)
        assertFalse(component.archive.value.failed)
    }

    private suspend fun component(exporter: VacancyArchiveExporter, finished: () -> Unit) =
        runOnUiThread {
            lifecycle.resume()
            val services = object : ApplicationDependencies by dependencies {
                override val vacancyArchiveUseCases = VacancyArchiveUseCases(
                    resumesRepository,
                    coverLettersRepository,
                    exporter,
                )
            }
            DefaultVacancyExportComponent(
                DefaultAppComponentContext(
                    DefaultComponentContext(lifecycle),
                    services,
                ),
                {
                    record
                },
                finished,
            )
        }
}
