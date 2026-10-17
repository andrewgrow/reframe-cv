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
import reframecv.ui.components.projects.vacancies.editor.VacancyDeleteState
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ObserveDashboardUseCase
import reframecv.usecase.VacanciesUseCases

class VacancyRemovalTest : ComponentTest() {
    private fun vacancy() =
        Vacancy(id = 12, projectId = 7, name = "Backend", createdAt = 100, updatedAt = 200)

    @Test
    fun requiresConfirmationAndBlocksOtherWritesUntilDeletionCompletes() = runBlocking {
        val original = vacancy()
        dependencies.vacanciesRepository.records.value = listOf(original)
        val writes = Channel<Long>(Channel.UNLIMITED)
        val finish = CompletableDeferred<Unit>()
        val closed = CompletableDeferred<Unit>()
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun delete(id: Long) {
                writes.send(id)
                finish.await()
                dependencies.vacanciesRepository.delete(id)
            }
        }
        runOnUiThread {
            lifecycle.resume()
            val component = DefaultVacanciesComponent(context(repository), 7, back = {})
            component.onEdit(original)
            component.childStack.subscribe {
                if (it.active.instance ==
                    VacanciesComponent.Child.List
                ) {
                    closed.complete(Unit)
                }
            }
            val editor = requireNotNull(component.editor)
            editor.onDelete()
            assertEquals(VacancyDeleteState.Confirming, editor.deleteState.value)
            assertEquals(true, writes.tryReceive().isFailure)
            editor.onDelete()
            assertEquals(VacancyDeleteState.Deleting, editor.deleteState.value)
            editor.onDelete()
            editor.onSave(VacancyDraft("Changed"))
            editor.onClose()
            component.onBack()
            assertEquals(editor, component.editor)
        }
        withTimeout(10_000.milliseconds) {
            assertEquals(original.id, writes.receive())
            assertEquals(true, writes.tryReceive().isFailure)
            finish.complete(Unit)
            closed.await()
        }
        assertEquals(emptyList(), dependencies.vacanciesRepository.records.value)
    }

    @Test
    fun failedDeletionCanBeRetriedWithoutLeavingPage() = runBlocking {
        val original = vacancy()
        dependencies.vacanciesRepository.records.value = listOf(original)
        val failure = IllegalStateException("Delete failed")
        var fail = true
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun delete(id: Long) {
                if (fail) throw failure
                dependencies.vacanciesRepository.delete(id)
            }
        }
        val states = Channel<VacancyDeleteState>(Channel.UNLIMITED)
        lateinit var component: DefaultVacanciesComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacanciesComponent(context(repository), 7, back = {})
            component.onEdit(original)
            val editor = requireNotNull(component.editor)
            editor.deleteState.subscribe { states.trySend(it) }
            editor.onDelete()
            editor.onDelete()
        }
        withTimeout(10_000.milliseconds) {
            while (states.receive() !=
                VacancyDeleteState.Failed
            ) { /* Wait for deletion failure. */ }
            assertEquals(listOf(original), dependencies.vacanciesRepository.records.value)
            val closed = CompletableDeferred<Unit>()
            val logged = dependencies.logger.failures.receive()
            assertSame(failure, logged.error)
            assertEquals("Delete vacancy", logged.operation)
            assertEquals(mapOf("projectId" to 7L, "vacancyId" to original.id), logged.context)
            runOnUiThread {
                fail = false
                component.childStack.subscribe {
                    if (it.active.instance ==
                        VacanciesComponent.Child.List
                    ) {
                        closed.complete(Unit)
                    }
                }
                requireNotNull(component.editor).onDelete()
            }
            closed.await()
        }
        assertEquals(emptyList(), dependencies.vacanciesRepository.records.value)
    }

    @Test
    fun cancelDismissesConfirmationAndCreationCannotDelete() = runOnUiThread {
        lifecycle.resume()
        val component = DefaultVacanciesComponent(appComponentContext(), 7, back = {})
        component.onEdit(vacancy())
        val editor = requireNotNull(component.editor)
        editor.onDelete()
        component.onBack()
        assertEquals(VacancyDeleteState.Idle, editor.deleteState.value)
        assertEquals(editor, component.editor)
        editor.onClose()
        assertNull(component.editor)
        component.onAdd()
        val creator = requireNotNull(component.editor)
        creator.onDelete()
        assertEquals(VacancyDeleteState.Idle, creator.deleteState.value)
    }

    private fun context(repository: VacanciesRepository) = DefaultAppComponentContext(
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
}
