package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.DefaultComponentContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.attachments.AttachmentPicker
import reframecv.attachments.AttachmentStatus
import reframecv.attachments.SelectedAttachment
import reframecv.attachments.SnapshotChange
import reframecv.attachments.SnapshotInfo
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.vacancyattachment.VacancyAttachment
import reframecv.repository.VacanciesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ObserveDashboardUseCase
import reframecv.usecase.VacanciesUseCases
import reframecv.usecase.VacancyFilesUseCases

class VacancySnapshotComponentTest : ComponentTest() {
    @Test
    fun selectionAndCancellationDoNotCopyFilesAndSavePassesSelection() = runBlocking {
        val selected = SelectedAttachment("original", "job.pdf")
        var pick: SelectedAttachment? = selected
        val services = object : ApplicationDependencies by dependencies {
            override val vacancyFilesUseCases get() = VacancyFilesUseCases(
                vacancyImporter,
                vacancyExporter,
                attachmentPicker,
                vacancyPdfExporter,
            )
            override val attachmentPicker = AttachmentPicker { pick }
        }
        val states = Channel<VacancySnapshotState>(Channel.UNLIMITED)
        val saved = Channel<VacancyDraft>(Channel.UNLIMITED)
        lateinit var editor: DefaultVacancyEditorComponent
        runOnUiThread {
            editor = DefaultVacancyEditorComponent(
                DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
                save = { saved.trySend(it) },
                close = {},
            )
            editor.snapshot.state.subscribe { states.trySend(it) }
            editor.snapshot.onAttach()
        }
        withTimeout(10_000) {
            while (states.receive().selected != selected) { /* Await selection. */ }
            pick = null
            runOnUiThread { editor.snapshot.onAttach() }
            do {
                val state = states.receive()
            } while (state.loading)
            runOnUiThread {
                assertEquals(selected, editor.snapshot.state.value.selected)
                editor.snapshot.onParsed()
                assertNotNull(editor.snapshot.state.value.parsedAt)
                editor.onSave(VacancyDraft("Title"))
            }
            val attached = assertIs<SnapshotChange.Attach>(saved.receive().snapshotChange)
            assertEquals(selected, attached.selection)
            assertNotNull(attached.parsedAt)
            assertTrueNoCopies()
            runOnUiThread {
                editor.snapshot.onRemove()
                assertEquals(attached, editor.snapshot.change)
                editor.onSaveCompleted(VacancySaveState.Failed)
                editor.snapshot.onRemove()
                assertEquals(SnapshotChange.Keep, editor.snapshot.change)
                assertNull(editor.snapshot.state.value.selected)
                assertNull(editor.snapshot.state.value.parsedAt)
            }
        }
    }

    @Test
    fun missingSnapshotKeepsMetadataAndRemoveIsPendingUntilSave() = runBlocking {
        val original = VacancyAttachment(
            id = 9,
            filePath = "attachments/source.pdf",
            mediaType = "application/pdf",
            fileSize = 100,
            sha256 = "hash",
            createdAt = 100,
            fileName = "missing.pdf",
            parsedAt = 200,
        )
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun snapshot(vacancyId: Long) =
                SnapshotInfo(original, AttachmentStatus.Missing)
        }
        val states = Channel<VacancySnapshotState>(Channel.UNLIMITED)
        lateinit var component: DefaultVacancySnapshotComponent
        runOnUiThread {
            val services = object : ApplicationDependencies by dependencies {
                override val vacanciesUseCases get() = VacanciesUseCases(vacanciesRepository)
                override val observeDashboard get() = ObserveDashboardUseCase(
                    resumesRepository,
                    vacanciesRepository,
                    coverLettersRepository,
                )
                override val vacanciesRepository = repository
            }
            component = DefaultVacancySnapshotComponent(
                DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
                1,
                true,
            )
            component.state.subscribe { states.trySend(it) }
        }
        withTimeout(10_000) {
            var state = states.receive()
            while (state.loading) state = states.receive()
            assertEquals(original, state.record)
            assertEquals(AttachmentStatus.Missing, state.status)
            assertEquals(200L, state.parsedAt)
            runOnUiThread {
                assertEquals(SnapshotChange.Keep, component.change)
                component.onRemove()
                assertEquals(SnapshotChange.Remove, component.change)
            }
            assertEquals(original, repository.snapshot(1).record)
            assertTrueNoCopies()
        }
    }

    private fun assertTrueNoCopies() {
        assertEquals(0, dependencies.attachmentStorage.files.size)
    }
}
