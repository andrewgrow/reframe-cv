package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.attachments.SelectedAttachment
import reframecv.attachments.SnapshotChange
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.vacancy.Vacancy
import reframecv.importing.ImportedVacancyFile
import reframecv.importing.VacancyImporter
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancyImportState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.VacancyFilesUseCases

class VacancyImportComponentTest : ComponentTest() {
    private val imported = Vacancy(
        projectId = 0,
        name = "Imported",
        createdAt = 0,
        updatedAt = 0,
        keywords = listOf("Kotlin", "Compose"),
        responsibilities = listOf("Build, ship"),
        requirements = listOf("Android"),
        niceToHave = listOf("German"),
    )

    @Test
    fun fillsDraftWithoutSavingAndBlocksActionsUntilImportCompletes() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val deleted = CompletableDeferred<Unit>()
        var calls = 0
        var saves = 0
        var closes = 0
        val component = editor(
            VacancyImporter {
                calls++
                release.await()
                file(deleted)
            },
            save = { saves++ },
            close = { closes++ },
        )
        val states = states(component)
        runOnUiThread {
            component.onDraftChange(VacancyDraft("Keep"))
            component.onImport()
            assertTrue(component.isBusy)
            component.onImport()
            component.onSave(VacancyDraft("Blocked"))
            component.onDraftChange(VacancyDraft("Blocked"))
            component.onClose()
            assertEquals("Keep", component.draft.value.name)
            assertEquals(0, saves)
            assertEquals(0, closes)
        }
        release.complete(Unit)
        withTimeout(10_000) {
            while (states.receive() != VacancyImportState.Imported) { /* Await parsing. */ }
            runOnUiThread {
                assertEquals(VacancyDraft.fromVacancy(imported), component.draft.value)
                assertFalse(component.isBusy)
                assertFalse(deleted.isCompleted)
                component.onSave(component.draft.value)
                assertEquals(1, saves)
                // Saving is stubbed here; the editor stays busy until the store replies.
            }
            runOnUiThread { lifecycle.destroy() }
            deleted.await()
            assertEquals(1, calls)
        }
    }

    @Test
    fun failureAndPickerCancellationPreserveDraftAndAllowRetry() = runBlocking {
        val error = IllegalArgumentException("Invalid document")
        val deleted = CompletableDeferred<Unit>()
        var calls = 0
        val component = editor(
            VacancyImporter {
                when (++calls) {
                    1 -> throw error
                    2 -> null
                    else -> file(deleted)
                }
            },
        )
        val states = states(component)
        runOnUiThread {
            component.onDraftChange(VacancyDraft("Keep"))
            component.onImport()
        }
        withTimeout(10_000) {
            while (states.receive() != VacancyImportState.Failed) { /* Await failure. */ }
            val logged = dependencies.logger.failures.receive()
            assertSame(error, logged.error)
            assertEquals("Import vacancy from JSON", logged.operation)
            runOnUiThread {
                assertEquals("Keep", component.draft.value.name)
                component.onImport()
            }
            while (states.receive() != VacancyImportState.Idle) { /* Await cancellation. */ }
            runOnUiThread {
                assertEquals("Keep", component.draft.value.name)
                component.onImport()
            }
            while (states.receive() != VacancyImportState.Imported) { /* Await retry. */ }
            runOnUiThread { component.onClose() }
            deleted.await()
        }
    }

    @Test
    fun preservesPendingSnapshotAndDisablesImportInManagementMode() = runBlocking {
        val deleted = CompletableDeferred<Unit>()
        val component = editor(VacancyImporter { file(deleted) })
        val states = states(component)
        val pending = SnapshotChange.Attach(SelectedAttachment("pending", "job.pdf"))
        runOnUiThread {
            component.onDraftChange(VacancyDraft("Manual", snapshotChange = pending))
            component.onImport()
        }
        withTimeout(10_000) {
            while (states.receive() != VacancyImportState.Imported) { /* Await import. */ }
            runOnUiThread {
                assertEquals(pending, component.draft.value.snapshotChange)
                component.onSaveCompleted(
                    reframecv.ui.components.projects.vacancies.editor.VacancySaveState.Idle,
                    imported.copy(id = 9, projectId = 7),
                )
                component.onImport()
                assertEquals(VacancyImportState.Idle, component.importComponent.state.value)
                component.onClose()
            }
            deleted.await()
        }
    }

    @Test
    fun destroyingEditorReleasesEverySuccessfullyImportedSource() = runBlocking {
        val deleted = List(2) { CompletableDeferred<Unit>() }
        var index = 0
        val component = editor(VacancyImporter { file(deleted[index++]) })
        val states = states(component)
        withTimeout(10_000) {
            repeat(2) {
                runOnUiThread { component.onImport() }
                while (states.receive() != VacancyImportState.Imported) { /* Await selection. */ }
            }
            runOnUiThread { lifecycle.destroy() }
            deleted.forEach { it.await() }
        }
    }

    private fun file(deleted: CompletableDeferred<Unit>) = object : ImportedVacancyFile {
        override val vacancy = imported
        override suspend fun deleteTemporaryFile() {
            deleted.complete(Unit)
        }
    }

    private fun states(component: DefaultVacancyEditorComponent) = runOnUiThread {
        Channel<VacancyImportState>(Channel.UNLIMITED).also { channel ->
            component.importComponent.state.subscribe { channel.trySend(it) }
        }
    }

    private fun editor(
        importer: VacancyImporter,
        save: (VacancyDraft) -> Unit = {},
        close: () -> Unit = {},
    ) = runOnUiThread {
        lifecycle.resume()
        val services = object : ApplicationDependencies by dependencies {
            override val vacancyFilesUseCases get() = VacancyFilesUseCases(
                vacancyImporter,
                vacancyExporter,
                attachmentPicker,
            )
            override val vacancyImporter = importer
        }
        DefaultVacancyEditorComponent(
            DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
            save,
            close,
        )
    }
}
