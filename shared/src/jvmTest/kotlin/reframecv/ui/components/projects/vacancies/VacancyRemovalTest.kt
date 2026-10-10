package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
            component.editorSlot.subscribe { if (it.child == null) closed.complete(Unit) }
            val editor = requireNotNull(component.editorSlot.value.child).instance
            editor.onDelete()
            assertEquals(VacancyDeleteState.Confirming, editor.deleteState.value)
            assertEquals(true, writes.tryReceive().isFailure)
            editor.onDelete()
            assertEquals(VacancyDeleteState.Deleting, editor.deleteState.value)
            editor.onDelete()
            editor.onSave(VacancyDraft("Changed"))
            editor.onClose()
            component.onBack()
            assertEquals(editor, component.editorSlot.value.child?.instance)
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
    fun failedDeletionCanBeRetriedWithoutLosingDialog() = runBlocking {
        val original = vacancy()
        dependencies.vacanciesRepository.records.value = listOf(original)
        var fail = true
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun delete(id: Long) {
                if (fail) error("Delete failed")
                dependencies.vacanciesRepository.delete(id)
            }
        }
        val states = Channel<VacancyDeleteState>(Channel.UNLIMITED)
        lateinit var component: DefaultVacanciesComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacanciesComponent(context(repository), 7, back = {})
            component.onEdit(original)
            val editor = requireNotNull(component.editorSlot.value.child).instance
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
            runOnUiThread {
                fail = false
                component.editorSlot.subscribe { if (it.child == null) closed.complete(Unit) }
                requireNotNull(component.editorSlot.value.child).instance.onDelete()
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
        val editor = requireNotNull(component.editorSlot.value.child).instance
        editor.onDelete()
        component.onBack()
        assertEquals(VacancyDeleteState.Idle, editor.deleteState.value)
        assertEquals(editor, component.editorSlot.value.child?.instance)
        editor.onClose()
        assertNull(component.editorSlot.value.child)
        component.onAdd()
        val creator = requireNotNull(component.editorSlot.value.child).instance
        creator.onDelete()
        assertEquals(VacancyDeleteState.Idle, creator.deleteState.value)
    }

    private fun context(repository: VacanciesRepository) = DefaultAppComponentContext(
        DefaultComponentContext(lifecycle),
        object : ApplicationDependencies by dependencies {
            override val vacanciesRepository = repository
        },
    )
}
