package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.DefaultComponentContext
import kotlin.test.Test
import kotlin.test.assertEquals
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

class VacancySnapshotComponentTest : ComponentTest() {
    @Test
    fun selectionAndCancellationDoNotCopyFilesAndSavePassesSelection() = runBlocking {
        val selected = SelectedAttachment("original", "job.pdf")
        var pick: SelectedAttachment? = selected
        val services = object : ApplicationDependencies by dependencies {
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
                editor.onSave(VacancyDraft("Title"))
            }
            assertEquals(SnapshotChange.Attach(selected), saved.receive().snapshotChange)
            assertTrueNoCopies()
            runOnUiThread {
                editor.snapshot.onRemove()
                assertEquals(SnapshotChange.Keep, editor.snapshot.change)
                assertNull(editor.snapshot.state.value.selected)
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
        )
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun snapshot(vacancyId: Long) =
                SnapshotInfo(original, AttachmentStatus.Missing)
        }
        val states = Channel<VacancySnapshotState>(Channel.UNLIMITED)
        lateinit var component: DefaultVacancySnapshotComponent
        runOnUiThread {
            val services = object : ApplicationDependencies by dependencies {
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
