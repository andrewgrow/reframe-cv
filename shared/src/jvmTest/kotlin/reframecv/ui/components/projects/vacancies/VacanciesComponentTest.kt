package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.vacancy.Vacancy
import reframecv.repository.VacanciesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class VacanciesComponentTest : ComponentTest() {
    @Test
    fun observesSelectedProjectAndCancelsOnDestroy() = runBlocking<Unit> {
        val records = MutableSharedFlow<List<Vacancy>>(replay = 1)
        val cancellations = Channel<Unit>(Channel.UNLIMITED)
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override fun observeVacancies(projectId: Long): Flow<List<Vacancy>> {
                assertEquals(7L, projectId)
                return records.onCompletion { cancellations.trySend(Unit) }
            }
        }
        val states = Channel<VacanciesState>(Channel.UNLIMITED)
        runOnUiThread {
            lifecycle.resume()
            val component = DefaultVacanciesComponent(context(repository), 7, back = {})
            component.uiState.subscribe { states.trySend(it) }
            assertIs<VacanciesState.Loading>(component.uiState.value)
        }
        val vacancy = Vacancy(id = 2, projectId = 7, name = "Kotlin", createdAt = 1, updatedAt = 1)
        records.emit(listOf(vacancy))
        withTimeout(10_000.milliseconds) {
            assertEquals(
                VacanciesState.Ready(listOf(vacancy)),
                states.awaitReady {
                    it.vacancies.isNotEmpty()
                },
            )
            records.emit(emptyList())
            assertEquals(
                VacanciesState.Ready(emptyList()),
                states.awaitReady {
                    it.vacancies.isEmpty()
                },
            )
            runOnUiThread { lifecycle.destroy() }
            cancellations.receive()
        }
        states.close()
        cancellations.close()
    }

    @Test
    fun failedReadCanBeRetriedAndReplacesPreviousObservation() = runBlocking<Unit> {
        var fail = true
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override fun observeVacancies(projectId: Long): Flow<List<Vacancy>> = flow {
                if (fail) error("Cannot read records")
                emit(emptyList())
            }
        }
        val states = Channel<VacanciesState>(Channel.UNLIMITED)
        lateinit var component: DefaultVacanciesComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacanciesComponent(context(repository), 7, back = {})
            component.uiState.subscribe { states.trySend(it) }
        }
        withTimeout(10_000.milliseconds) {
            while (states.receive() !=
                VacanciesState.LoadFailed
            ) { /* Wait for the failed read. */ }
            runOnUiThread {
                fail = false
                component.onRetry()
                component.onRetry()
            }
            while (states.receive() !is VacanciesState.Ready) { /* Wait for the retry. */ }
        }
        runOnUiThread { assertEquals(VacanciesState.Ready(emptyList()), component.uiState.value) }
        states.close()
    }

    private suspend fun Channel<VacanciesState>.awaitReady(
        predicate: (VacanciesState.Ready) -> Boolean,
    ): VacanciesState.Ready {
        while (true) {
            val state = receive()
            if (state is VacanciesState.Ready && predicate(state)) return state
        }
    }

    private fun context(repository: VacanciesRepository) = DefaultAppComponentContext(
        DefaultComponentContext(lifecycle),
        object : ApplicationDependencies by dependencies {
            override val vacanciesRepository = repository
        },
    )
}
