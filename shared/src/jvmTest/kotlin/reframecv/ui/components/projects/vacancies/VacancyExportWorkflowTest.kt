package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
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
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.vacancy.Vacancy
import reframecv.export.DocumentExportResult
import reframecv.export.VacancyExporter
import reframecv.repository.VacanciesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class VacancyExportWorkflowTest : ComponentTest() {
    private val original = Vacancy(
        id = 7,
        projectId = 1,
        name = "Old",
        createdAt = 10,
        updatedAt = 20,
        requirements = listOf("Kotlin"),
        responsibilities = listOf("Build apps"),
        niceToHave = listOf("CI/CD"),
    )

    @Test
    fun saveFailureKeepsDraftThenRetryExportsCurrentContentWithoutLeavingThePage() = runBlocking {
        dependencies.vacanciesRepository.records.value = listOf(original)
        var fail = true
        var writes = 0
        val exported = Channel<Vacancy>(Channel.UNLIMITED)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun update(record: Vacancy) {
                writes++
                if (fail) error("Disk full")
                entered.complete(Unit)
                release.await()
                dependencies.vacanciesRepository.update(record)
            }
        }
        val services = object : ApplicationDependencies by dependencies {
            override val vacanciesRepository = repository
            override val vacancyExporter = VacancyExporter {
                exported.send(it)
                DocumentExportResult.Cancelled
            }
        }
        val component = component(services)
        val editor = checkNotNull(component.editor)
        val visible = CompletableDeferred<Unit>()
        runOnUiThread {
            editor.exportVisible.subscribe { if (it) visible.complete(Unit) }
            editor.onDraftChange(editor.draft.value.copy(title = "Updated", company = "Company"))
            editor.onExport()
        }
        withTimeout(10_000) {
            dependencies.logger.failures.receive()
            runOnUiThread {
                assertEquals(VacancySaveState.Failed, editor.saveState.value)
                assertFalse(editor.exportVisible.value)
                fail = false
                editor.onExport()
                editor.onExport()
                editor.onClose()
                assertSame(editor, component.editor)
            }
            entered.await()
            assertEquals(2, writes)
            release.complete(Unit)
            visible.await()
            runOnUiThread { editor.exportComponent.onDownload() }
            val record = exported.receive()
            assertEquals(dependencies.vacanciesRepository.records.value.single(), record)
            assertEquals(original.requirements, record.requirements)
            runOnUiThread {
                assertSame(editor, component.editor)
                assertTrue(editor.draft.value.title == "Updated")
            }
        }
    }
    private fun component(services: ApplicationDependencies) = runOnUiThread {
        lifecycle.resume()
        DefaultVacanciesComponent(
            DefaultAppComponentContext(
                DefaultComponentContext(lifecycle),
                services,
            ),
            1,
            back = {
            },
        )
            .also { it.onEdit(original) }
    }
}
