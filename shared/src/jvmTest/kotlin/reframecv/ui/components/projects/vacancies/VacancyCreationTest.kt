package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
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

class VacancyCreationTest : ComponentTest() {
    @Test
    fun savesToSelectedProjectOnceAndClosesDialogAfterSuccess() = runBlocking {
        val writes = Channel<Vacancy>(Channel.UNLIMITED)
        val finish = CompletableDeferred<Unit>()
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun create(record: Vacancy): Long {
                writes.send(record)
                finish.await()
                return dependencies.vacanciesRepository.create(record)
            }
        }
        lateinit var component: DefaultVacanciesComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacanciesComponent(context(repository), 7, back = {})
            component.onAdd()
            val editor = requireNotNull(component.editorSlot.value.child).instance
            editor.onSave(VacancyDraft("   "))
            assertEquals(VacancySaveState.Idle, editor.saveState.value)
            editor.onSave(
                VacancyDraft(
                    " Kotlin ",
                    " Example ",
                    " Description ",
                    " url ",
                    " Kotlin, , Android ",
                ),
            )
            assertEquals(VacancySaveState.Saving, editor.saveState.value)
            editor.onSave(VacancyDraft("Duplicate"))
            editor.onClose()
            (component.backHandler as BackDispatcher).back()
            assertEquals(editor, component.editorSlot.value.child?.instance)
        }
        withTimeout(10_000.milliseconds) {
            val saved = writes.receive()
            assertEquals(
                VacancyDraft(
                    "Kotlin",
                    "Example",
                    "Description",
                    "url",
                    "Kotlin,Android",
                ).toVacancy(7),
                saved,
            )
            assertEquals(true, writes.tryReceive().isFailure)
            val closed = CompletableDeferred<Unit>()
            runOnUiThread {
                component.editorSlot.subscribe { if (it.child == null) closed.complete(Unit) }
                finish.complete(Unit)
            }
            closed.await()
        }
        runOnUiThread {
            assertNull(component.editorSlot.value.child)
            assertEquals(1, dependencies.vacanciesRepository.records.value.size)
        }
    }

    @Test
    fun failedSaveCanBeRetriedAndCancelDoesNotNavigateAway() = runBlocking {
        var fail = true
        var backs = 0
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun create(record: Vacancy): Long {
                if (fail) error("Write failed")
                return dependencies.vacanciesRepository.create(record)
            }
        }
        val states = Channel<VacancySaveState>(Channel.UNLIMITED)
        lateinit var component: DefaultVacanciesComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacanciesComponent(context(repository), 7, back = { backs++ })
            component.onAdd()
            val editor = requireNotNull(component.editorSlot.value.child).instance
            editor.saveState.subscribe { states.trySend(it) }
            editor.onSave(VacancyDraft("Backend"))
        }
        withTimeout(10_000) {
            while (states.receive() != VacancySaveState.Failed) { /* Wait for write failure. */ }
            val closed = CompletableDeferred<Unit>()
            runOnUiThread {
                fail = false
                component.editorSlot.subscribe { if (it.child == null) closed.complete(Unit) }
                requireNotNull(
                    component.editorSlot.value.child,
                ).instance.onSave(VacancyDraft("Backend"))
            }
            closed.await()
        }
        runOnUiThread {
            component.onAdd()
            (component.backHandler as BackDispatcher).back()
            assertNull(component.editorSlot.value.child)
            assertEquals(0, backs)
            component.onBack()
            assertEquals(1, backs)
        }
    }

    @Test
    fun destroyingComponentCancelsPendingSave() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun create(record: Vacancy): Long {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
        }
        runOnUiThread {
            lifecycle.resume()
            val component = DefaultVacanciesComponent(context(repository), 7, back = {})
            component.onAdd()
            requireNotNull(
                component.editorSlot.value.child,
            ).instance.onSave(VacancyDraft("Backend"))
        }
        withTimeout(10_000) {
            started.await()
            runOnUiThread { lifecycle.destroy() }
            cancelled.await()
        }
        assertEquals(emptyList(), dependencies.vacanciesRepository.records.value)
    }

    private fun context(repository: VacanciesRepository) = DefaultAppComponentContext(
        DefaultComponentContext(lifecycle),
        object : ApplicationDependencies by dependencies {
            override val vacanciesRepository = repository
        },
    )
}
