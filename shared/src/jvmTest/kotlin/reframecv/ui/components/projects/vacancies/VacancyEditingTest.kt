package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.vacancy.Vacancy
import reframecv.repository.VacanciesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ObserveDashboardUseCase
import reframecv.usecase.VacanciesUseCases

class VacancyEditingTest : ComponentTest() {
    private fun original() = Vacancy(
        id = 12, projectId = 7, name = "Old title", company = "Old company",
        description = "Old description", url = "old url", keywords = listOf("Old"),
        createdAt = 100, updatedAt = 200, resumeId = 3, coverLetterId = 4, vacancyAttachmentId = 5,
    )

    @Test
    fun updatesExistingRecordAndPreservesIdentityTimestampsAndLinks() = runBlocking {
        val original = original()
        val another = original.copy(id = 13, name = "Another")
        dependencies.vacanciesRepository.records.value = listOf(original, another)
        val saved = CompletableDeferred<Unit>()
        lateinit var component: DefaultVacanciesComponent
        val draft =
            VacancyDraft(
                " New title ",
                " New company ",
                " New description ",
                " new url ",
                " Kotlin, Android ",
            )
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacanciesComponent(appComponentContext(), 7, back = {})
            component.onEdit(original)
            val editor = requireNotNull(component.editor)
            assertEquals(original, editor.initialVacancy)
            editor.onSave(draft)
            editor.saveState.subscribe {
                if (it == VacancySaveState.Idle) saved.complete(Unit)
            }
        }
        withTimeout(10_000.milliseconds) { saved.await() }
        runOnUiThread {
            assertSame(original, requireNotNull(component.editor).initialVacancy)
            assertEquals(
                listOf(draft.applyTo(original), another),
                dependencies.vacanciesRepository.records.value,
            )
        }
    }

    @Test
    fun failedUpdateStaysOpenAndCanBeRetried() = runBlocking {
        val original = original()
        dependencies.vacanciesRepository.records.value = listOf(original)
        val failure = IllegalStateException("Update failed")
        var fail = true
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun update(record: Vacancy) {
                if (fail) throw failure
                dependencies.vacanciesRepository.update(record)
            }
        }
        val states = Channel<VacancySaveState>(Channel.UNLIMITED)
        lateinit var component: DefaultVacanciesComponent
        val draft = VacancyDraft("New title")
        runOnUiThread {
            lifecycle.resume()
            val context = DefaultAppComponentContext(
                DefaultComponentContext(lifecycle),
                object : ApplicationDependencies by dependencies {
                    override val vacanciesUseCases get() = VacanciesUseCases(vacanciesRepository)
                    override val observeDashboard get() = ObserveDashboardUseCase(
                        resumesRepository,
                        vacanciesRepository,
                        coverLettersRepository,
                    )
                    override val vacanciesRepository = repository
                },
            )
            component = DefaultVacanciesComponent(context, 7, back = {})
            component.onEdit(original)
            val editor = requireNotNull(component.editor)
            editor.saveState.subscribe { states.trySend(it) }
            editor.onSave(draft)
        }
        withTimeout(10_000.milliseconds) {
            while (states.receive() != VacancySaveState.Failed) { /* Wait for update failure. */ }
            assertEquals(listOf(original), dependencies.vacanciesRepository.records.value)
            val saved = CompletableDeferred<Unit>()
            val logged = dependencies.logger.failures.receive()
            assertSame(failure, logged.error)
            assertEquals("Update vacancy", logged.operation)
            assertEquals(mapOf("projectId" to 7L, "vacancyId" to 12L), logged.context)
            runOnUiThread {
                fail = false
                val editor = requireNotNull(component.editor)
                editor.onSave(draft)
                editor.saveState.subscribe {
                    if (it == VacancySaveState.Idle) saved.complete(Unit)
                }
            }
            saved.await()
            runOnUiThread { assertSame(original, requireNotNull(component.editor).initialVacancy) }
        }
        assertEquals(
            listOf(draft.applyTo(original)),
            dependencies.vacanciesRepository.records.value,
        )
    }

    @Test
    fun cancellationPreservesRecordAndRejectsDifferentProject() = runOnUiThread {
        lifecycle.resume()
        val component = DefaultVacanciesComponent(appComponentContext(), 7, back = {})
        val original = original()
        dependencies.vacanciesRepository.records.value = listOf(original)
        component.onEdit(original.copy(projectId = 8))
        assertNull(component.editor)
        component.onEdit(original)
        requireNotNull(component.editor).onClose()
        assertNull(component.editor)
        assertEquals(listOf(original), dependencies.vacanciesRepository.records.value)
    }
}
