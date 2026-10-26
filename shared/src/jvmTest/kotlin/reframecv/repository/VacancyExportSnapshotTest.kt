package reframecv.repository

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.attachments.AttachmentPicker
import reframecv.attachments.SelectedAttachment
import reframecv.attachments.SnapshotChange
import reframecv.attachments.TestAttachmentStorage
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.dependencies.ApplicationDependencies
import reframecv.dependencies.TestApplicationDependencies
import reframecv.domain.models.vacancy.Vacancy
import reframecv.logging.ApplicationLogger
import reframecv.ui.components.projects.vacancies.DefaultVacanciesComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ObserveDashboardUseCase
import reframecv.usecase.VacanciesUseCases
import reframecv.usecase.VacancyFilesUseCases

class VacancyExportSnapshotTest : WorkspaceDatabaseTest() {
    @Test
    fun repeatedExportCommitsPendingAttachmentOnceAndKeepsItsSavedMetadata() = runBlocking {
        val storage = TestAttachmentStorage()
        val repository = repository(storage)
        val projectId = project()
        repository.create(vacancy(projectId))
        val original = repository.observeVacancies(projectId).first().single()
        val lifecycle = LifecycleRegistry()
        val component = component(lifecycle, repository, original)
        try {
            val editor = checkNotNull(component.editor)
            val selected = Channel<Unit>(Channel.CONFLATED)
            val visible = Channel<Boolean>(Channel.UNLIMITED)
            val loaded = Channel<Unit>(Channel.CONFLATED)
            runOnUiThread {
                editor.snapshot.state.subscribe {
                    if (it.selected != null && !it.loading) selected.trySend(Unit)
                    if (it.record != null && !it.loading) loaded.trySend(Unit)
                }
                editor.exportVisible.subscribe(visible::trySend)
                editor.snapshot.onAttach()
            }
            withTimeout(10_000) {
                selected.receive()
                runOnUiThread { editor.onExport() }
                while (!visible.receive()) { /* Await successful Save. */ }
                loaded.receive()
                val source =
                    checkNotNull(
                        repository.observeVacancies(projectId).first().single().vacancyAttachmentId,
                    )
                runOnUiThread {
                    assertEquals(SnapshotChange.Keep, editor.snapshot.change)
                    assertEquals(source, editor.snapshot.state.value.record?.id)
                    editor.onCloseExport()
                    editor.onExport()
                }
                while (!visible.receive()) { /* Await repeated Save. */ }
                assertEquals(
                    source,
                    repository.observeVacancies(projectId).first().single().vacancyAttachmentId,
                )
                assertEquals(1, storage.files.size)
                runOnUiThread {
                    assertSame(editor, component.editor)
                    editor.onCloseExport()
                    editor.snapshot.onRemove()
                    assertEquals(SnapshotChange.Remove, editor.snapshot.change)
                }
            }
        } finally {
            runOnUiThread { lifecycle.destroy() }
        }
    }

    @Test
    fun newlyCreatedVacancyKeepsIdentityAndAttachmentThroughExportAndDeletion() = runBlocking {
        val storage = TestAttachmentStorage()
        val repository = repository(storage)
        val projectId = project()
        val lifecycle = LifecycleRegistry()
        val component = component(lifecycle, repository, vacancy(projectId), create = true)
        try {
            val editor = checkNotNull(component.editor)
            val loaded = Channel<Unit>(Channel.CONFLATED)
            val selected = Channel<Unit>(Channel.CONFLATED)
            val visible = Channel<Boolean>(Channel.UNLIMITED)
            runOnUiThread {
                editor.snapshot.state.subscribe {
                    if (it.selected != null && !it.loading) selected.trySend(Unit)
                    if (it.record != null && !it.loading) loaded.trySend(Unit)
                }
                editor.exportVisible.subscribe(visible::trySend)
                editor.onDraftChange(VacancyDraft("New vacancy"))
                editor.snapshot.onAttach()
            }
            withTimeout(10_000) {
                selected.receive()
                runOnUiThread { editor.onSave(editor.draft.value) }
                loaded.receive()
                val saved = repository.observeVacancies(projectId).first().single()
                runOnUiThread { assertSame(editor, component.editor) }
                verifyCreatedVacancy(editor, saved)
                // Loading/opening shares the snapshot state; wait for completion before exporting.
                loaded.receive()
                runOnUiThread {
                    editor.onDraftChange(editor.draft.value.copy(name = "Updated before export"))
                    editor.onExport()
                }
                while (!visible.receive()) { /* Await the export save. */ }
                val updated = repository.observeVacancies(projectId).first().single()
                assertEquals(saved.id, updated.id)
                assertEquals(saved.createdAt, updated.createdAt)
                assertEquals(saved.vacancyAttachmentId, updated.vacancyAttachmentId)
                assertEquals("Updated before export", updated.name)
                assertEquals(1, storage.files.size)
                runOnUiThread {
                    assertNotNull(editor.initialVacancy)
                    editor.exportComponent.onDownload()
                }
                while (visible.receive()) { /* Await download cancellation. */ }
                deleteCreatedVacancy(component)
                assertEquals(emptyList(), repository.observeVacancies(projectId).first())
                assertEquals(0, storage.files.size)
                assertNull(
                    database.vacancyAttachmentDao().findById(
                        checkNotNull(saved.vacancyAttachmentId),
                    ),
                )
            }
        } finally {
            runOnUiThread { lifecycle.destroy() }
        }
    }

    private fun verifyCreatedVacancy(editor: VacancyEditorComponent, saved: Vacancy) =
        runOnUiThread {
            assertEquals(saved, editor.initialVacancy)
            assertEquals(VacancySaveState.Idle, editor.saveState.value)
            assertEquals(SnapshotChange.Keep, editor.snapshot.change)
            assertEquals(saved.vacancyAttachmentId, editor.snapshot.state.value.record?.id)
            editor.snapshot.onOpen()
        }

    private suspend fun deleteCreatedVacancy(component: DefaultVacanciesComponent) {
        val closed = kotlinx.coroutines.CompletableDeferred<Unit>()
        runOnUiThread {
            val editor = checkNotNull(component.editor)
            component.childStack.subscribe {
                if (component.editor == null) closed.complete(Unit)
            }
            editor.onDelete()
            editor.onDelete()
        }
        closed.await()
    }

    private fun repository(storage: TestAttachmentStorage) = LocalVacanciesRepository(
        database.vacancyDao(),
        VacancySnapshotManager(
            database.vacancyDao(),
            database.vacancyAttachmentDao(),
            storage,
            ApplicationLogger.None,
        ),
    )

    private fun component(
        lifecycle: LifecycleRegistry,
        repository: VacanciesRepository,
        original: Vacancy,
        create: Boolean = false,
    ): DefaultVacanciesComponent {
        val services = object : ApplicationDependencies by TestApplicationDependencies() {
            override val vacanciesUseCases get() = VacanciesUseCases(vacanciesRepository)
            override val observeDashboard get() = ObserveDashboardUseCase(
                resumesRepository,
                vacanciesRepository,
                coverLettersRepository,
            )
            override val vacancyFilesUseCases get() = VacancyFilesUseCases(
                vacancyImporter,
                vacancyExporter,
                attachmentPicker,
                vacancyPdfExporter,
            )
            override val vacanciesRepository = repository
            override val attachmentPicker =
                AttachmentPicker { SelectedAttachment("source", "job.txt") }
        }
        return runOnUiThread {
            lifecycle.resume()
            DefaultVacanciesComponent(
                DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
                original.projectId,
                back = {
                },
            ).also { if (create) it.onAdd() else it.onEdit(original) }
        }
    }
}
