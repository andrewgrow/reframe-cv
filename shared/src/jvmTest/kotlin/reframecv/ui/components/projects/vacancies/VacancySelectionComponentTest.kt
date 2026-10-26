package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.vacancy.Vacancy
import reframecv.repository.VacanciesRepository
import reframecv.shared.nowMillis
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.VacanciesUseCases

class VacancySelectionComponentTest : ComponentTest() {
    private val records = listOf(Vacancy(1, 7, "Main", 0, 0), Vacancy(2, 7, "Adapted", 0, 0))
    private val state = MutableValue<VacanciesState>(VacanciesState.Ready(records))

    @Test
    fun selectsOnlyVisibleVacanciesSupportsSelectAllAndPrunesRemovedRecords() = runBlocking<Unit> {
        runOnUiThread {
            lifecycle.resume()
            val component =
                DefaultVacancySelectionComponent(appComponentContext(), 7, state) { true }
            component.onToggle(999)
            assertTrue(component.state.value.selected.isEmpty())
            component.onToggle(1)
            assertEquals(setOf(1L), component.state.value.selected)
            component.onToggleAll()
            assertEquals(setOf(1L, 2L), component.state.value.selected)
            component.onToggleAll()
            assertTrue(component.state.value.selected.isEmpty())
            component.onToggle(2)
            component.onRequestDelete()
            component.onToggle(1)
            assertEquals(setOf(2L), component.state.value.selected)
            component.onCancelDelete()
            assertFalse(component.state.value.confirming)
            state.value = VacanciesState.Ready(listOf(records.first()))
            assertTrue(component.state.value.selected.isEmpty())
        }
    }

    @Test
    fun failureKeepsSelectionAndConfirmationForRetryAndBusyStateBlocksDismissal() =
        runBlocking<Unit> {
            dependencies.vacanciesRepository.records.value = records
            var fail = true
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
                override suspend fun deleteMany(projectId: Long, ids: Set<Long>) {
                    if (fail) error("Database unavailable")
                    started.complete(Unit)
                    release.await()
                    dependencies.vacanciesRepository.deleteMany(projectId, ids)
                }
            }
            val failed = CompletableDeferred<Unit>()
            val completed = CompletableDeferred<Unit>()
            val component = runOnUiThread {
                lifecycle.resume()
                val context =
                    DefaultAppComponentContext(
                        com.arkivanov.decompose.DefaultComponentContext(lifecycle),
                        object : ApplicationDependencies by dependencies {
                            override val vacanciesUseCases = VacanciesUseCases(repository)
                        },
                    )
                DefaultVacancySelectionComponent(context, 7, state) { true }.also {
                    it.state.subscribe { value ->
                        if (value.failed) failed.complete(Unit)
                        if (started.isCompleted && !value.confirming) completed.complete(Unit)
                    }
                    it.onToggleAll()
                    it.onRequestDelete()
                    it.onConfirmDelete()
                }
            }
            withTimeout(5000) { failed.await() }
            runOnUiThread {
                assertTrue(component.state.value.confirming)
                assertEquals(setOf(1L, 2L), component.state.value.selected)
                assertEquals(2, dependencies.vacanciesRepository.records.value.size)
                fail = false
                component.onConfirmDelete()
            }
            withTimeout(5000) { started.await() }
            runOnUiThread {
                component.onCancelDelete()
                component.onToggleAll()
                assertTrue(component.state.value.deleting)
                assertTrue(component.state.value.confirming)
            }
            release.complete(Unit)
            withTimeout(5000) { completed.await() }
            assertTrue(dependencies.vacanciesRepository.records.value.isEmpty())
        }

    @Test
    fun bulkAppliedCapturesClickTimeBlocksRepeatedActionsAndSupportsRetry() = runBlocking<Unit> {
        dependencies.vacanciesRepository.records.value = records
        val started = CompletableDeferred<Long>()
        val release = CompletableDeferred<Unit>()
        val failed = CompletableDeferred<Unit>()
        val completed = CompletableDeferred<Unit>()
        var fail = true
        var calls = 0
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun markApplied(projectId: Long, ids: Set<Long>, timestamp: Long) {
                calls++
                assertEquals(setOf(1L, 2L), ids)
                if (fail) error("Unavailable")
                started.complete(timestamp)
                release.await()
                dependencies.vacanciesRepository.markApplied(projectId, ids, timestamp)
            }
        }
        val before = nowMillis()
        val component = runOnUiThread {
            lifecycle.resume()
            val context = DefaultAppComponentContext(
                com.arkivanov.decompose.DefaultComponentContext(lifecycle),
                object : ApplicationDependencies by dependencies {
                    override val vacanciesUseCases = VacanciesUseCases(repository)
                },
            )
            DefaultVacancySelectionComponent(context, 7, state) { true }.also {
                it.state.subscribe { value ->
                    if (value.applicationFailed) failed.complete(Unit)
                    if (started.isCompleted && !value.busy) completed.complete(Unit)
                }
                it.onMarkApplied()
                assertEquals(0, calls)
                it.onToggleAll()
                it.onMarkApplied()
            }
        }
        withTimeout(5000) { failed.await() }
        runOnUiThread {
            assertEquals(setOf(1L, 2L), component.state.value.selected)
            fail = false
            component.onMarkApplied()
        }
        val timestamp = withTimeout(5000) { started.await() }
        assertTrue(timestamp in before..nowMillis())
        runOnUiThread {
            component.onToggleAll()
            component.onRequestDelete()
            component.onMarkApplied()
            assertTrue(component.state.value.busy)
            assertFalse(component.state.value.confirming)
            assertEquals(setOf(1L, 2L), component.state.value.selected)
            assertEquals(2, calls)
        }
        release.complete(Unit)
        withTimeout(5000) { completed.await() }
        assertTrue(dependencies.vacanciesRepository.records.value.all { it.appliedAt == timestamp })
    }

    @Test
    fun appliedFilterPrunesHiddenSelectionsAndSelectAllUsesOnlyVisibleRows() = runBlocking<Unit> {
        runOnUiThread {
            lifecycle.resume()
            state.value = VacanciesState.Ready(listOf(records[0].copy(appliedAt = 123), records[1]))
            val component =
                DefaultVacancySelectionComponent(appComponentContext(), 7, state) { true }
            component.onToggleAll()
            component.onToggleAppliedFilter()
            assertTrue(component.state.value.hideApplied)
            assertEquals(setOf(2L), component.state.value.selected)
            component.onToggle(1)
            assertEquals(setOf(2L), component.state.value.selected)
            component.onToggleAll()
            assertTrue(component.state.value.selected.isEmpty())
            component.onToggleAll()
            assertEquals(setOf(2L), component.state.value.selected)
            state.value = VacanciesState.Ready(records.map { it.copy(appliedAt = 456) })
            assertTrue(component.state.value.selected.isEmpty())
            component.onToggleAppliedFilter()
            component.onToggleAll()
            assertEquals(setOf(1L, 2L), component.state.value.selected)
            component.onRequestDelete()
            component.onToggleAppliedFilter()
            assertFalse(component.state.value.hideApplied)
        }
    }

    @Test
    fun applyingWhileFilteredClearsSelectionEvenWhenObservationUpdatesDuringTheWrite() =
        runBlocking<Unit> {
            val completed = CompletableDeferred<Unit>()
            val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
                override suspend fun markApplied(projectId: Long, ids: Set<Long>, timestamp: Long) {
                    state.value =
                        VacanciesState.Ready(records.map { it.copy(appliedAt = timestamp) })
                }
            }
            val component = runOnUiThread {
                lifecycle.resume()
                val context = DefaultAppComponentContext(
                    com.arkivanov.decompose.DefaultComponentContext(lifecycle),
                    object : ApplicationDependencies by dependencies {
                        override val vacanciesUseCases = VacanciesUseCases(repository)
                    },
                )
                DefaultVacancySelectionComponent(context, 7, state) { true }.also {
                    it.state.subscribe { value ->
                        if (!value.busy && value.selected.isEmpty() &&
                            (state.value as VacanciesState.Ready).vacancies.all { v ->
                                v.appliedAt !=
                                    null
                            }
                        ) {
                            completed.complete(Unit)
                        }
                    }
                    it.onToggleAppliedFilter()
                    it.onToggleAll()
                    it.onMarkApplied()
                }
            }
            withTimeout(5000) { completed.await() }
            assertTrue(component.state.value.hideApplied)
            assertTrue(component.state.value.selected.isEmpty())
            assertTrue(
                component.state.value.visible((state.value as VacanciesState.Ready).vacancies)
                    .isEmpty(),
            )
        }
}
