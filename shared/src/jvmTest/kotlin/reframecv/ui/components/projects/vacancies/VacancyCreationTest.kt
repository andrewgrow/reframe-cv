package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.attachments.SnapshotChange
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

class VacancyCreationTest : ComponentTest() {
    @Test
    fun firstSaveKeepsEditorAndSecondSaveUpdatesCreatedVacancy() = runBlocking {
        val writes = Channel<Vacancy>(Channel.UNLIMITED)
        val finish = CompletableDeferred<Unit>()
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun createSaved(record: Vacancy, snapshot: SnapshotChange): Vacancy {
                writes.send(record)
                finish.await()
                return dependencies.vacanciesRepository.createSaved(record, snapshot)
            }
        }
        lateinit var component: DefaultVacanciesComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacanciesComponent(context(repository), 7, back = {})
            component.onAdd()
            val editor = requireNotNull(component.editor)
            editor.onSave(VacancyDraft("   "))
            assertEquals(VacancySaveState.Idle, editor.saveState.value)
            val draft = VacancyDraft(
                " Kotlin ",
                " Example ",
                " Description ",
                " url ",
                " Kotlin, , Android ",
            )
            editor.onDraftChange(draft)
            editor.onSave(draft)
            assertEquals(VacancySaveState.Saving, editor.saveState.value)
            editor.onSave(VacancyDraft("Duplicate"))
            editor.onClose()
            (component.backHandler as BackDispatcher).back()
            assertEquals(editor, component.editor)
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
            val savedState = CompletableDeferred<Unit>()
            runOnUiThread {
                val editor = requireNotNull(component.editor)
                editor.saveState.subscribe {
                    if (it == VacancySaveState.Idle) savedState.complete(Unit)
                }
                finish.complete(Unit)
            }
            savedState.await()
        }
        updateCreatedVacancy(component)
        runOnUiThread {
            assertNotNull(component.editor)
            assertEquals("Updated", dependencies.vacanciesRepository.records.value.single().name)
            assertEquals(true, writes.tryReceive().isFailure)
        }
    }

    @Test
    fun failedSaveCanBeRetriedAndCancelDoesNotNavigateAway() = runBlocking {
        val failure = IllegalStateException("Write failed")
        var fail = true
        var backs = 0
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun createSaved(record: Vacancy, snapshot: SnapshotChange): Vacancy {
                if (fail) throw failure
                return dependencies.vacanciesRepository.createSaved(record, snapshot)
            }
        }
        val states = Channel<VacancySaveState>(Channel.UNLIMITED)
        lateinit var component: DefaultVacanciesComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacanciesComponent(context(repository), 7, back = { backs++ })
            component.onAdd()
            val editor = requireNotNull(component.editor)
            editor.saveState.subscribe { states.trySend(it) }
            editor.onSave(VacancyDraft("Backend"))
        }
        withTimeout(10_000.milliseconds) {
            while (states.receive() != VacancySaveState.Failed) { /* Wait for write failure. */ }
            val savedState = CompletableDeferred<Unit>()
            val logged = dependencies.logger.failures.receive()
            assertSame(failure, logged.error)
            assertEquals("Create vacancy", logged.operation)
            assertEquals(mapOf("projectId" to 7L, "vacancyId" to null), logged.context)
            runOnUiThread {
                fail = false
                val editor = requireNotNull(component.editor)
                assertNull(editor.initialVacancy)
                editor.saveState.subscribe {
                    if (it == VacancySaveState.Idle) savedState.complete(Unit)
                }
                editor.onSave(VacancyDraft("Backend"))
            }
            savedState.await()
        }
        runOnUiThread {
            assertNotNull(component.editor?.initialVacancy)
            (component.backHandler as BackDispatcher).back()
            assertNull(component.editor)
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
            override suspend fun createSaved(record: Vacancy, snapshot: SnapshotChange): Vacancy {
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
                component.editor,
            ).onSave(VacancyDraft("Backend"))
        }
        withTimeout(10_000.milliseconds) {
            started.await()
            runOnUiThread { lifecycle.destroy() }
            cancelled.await()
        }
        assertEquals(emptyList(), dependencies.vacanciesRepository.records.value)
    }

    private suspend fun updateCreatedVacancy(component: DefaultVacanciesComponent) {
        val completed = CompletableDeferred<Unit>()
        lateinit var editor:
            reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
        runOnUiThread {
            editor = assertNotNull(component.editor)
            val saved = assertNotNull(editor.initialVacancy)
            assertEquals(dependencies.vacanciesRepository.records.value.single(), saved)
            assertEquals(" Kotlin ", editor.draft.value.name)
            assertEquals(SnapshotChange.Keep, editor.snapshot.change)
            editor.onSave(editor.draft.value.copy(name = "Updated"))
            editor.saveState.subscribe {
                if (it == VacancySaveState.Idle) completed.complete(Unit)
            }
        }
        withTimeout(10_000) { completed.await() }
        runOnUiThread { assertSame(editor, component.editor) }
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
