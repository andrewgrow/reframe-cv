package reframecv.ui.components.projects.dashboard

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
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.repository.ResumesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class DashboardComponentTest : ComponentTest() {
    @Test
    fun waitsForEverySectionObservesUpdatesAndCancelsOnDestroy() = runBlocking<Unit> {
        val resumes = MutableSharedFlow<List<Resume>>(replay = 1)
        val cancellations = Channel<Unit>(Channel.UNLIMITED)
        val repository = object : ResumesRepository by dependencies.resumesRepository {
            override fun observeResumes(projectId: Long): Flow<List<Resume>> {
                assertEquals(7L, projectId)
                return resumes.onCompletion { cancellations.send(Unit) }
            }
        }
        val states = Channel<DashboardState>(Channel.UNLIMITED)
        lateinit var component: DefaultDashboardComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultDashboardComponent(context(repository), 7)
            component.uiState.subscribe { states.trySend(it) }
            assertIs<DashboardState.Loading>(component.uiState.value)
        }
        val resume = Resume(id = 1, projectId = 7, name = "Backend", createdAt = 1, updatedAt = 1)
        val vacancy = Vacancy(id = 2, projectId = 7, name = "Kotlin", createdAt = 1, updatedAt = 1)
        val letter =
            CoverLetter(id = 3, projectId = 7, name = "Hello", createdAt = 1, updatedAt = 1)
        dependencies.vacanciesRepository.records.value =
            listOf(vacancy, vacancy.copy(id = 4, projectId = 8))
        dependencies.coverLettersRepository.records.value = listOf(letter)
        resumes.emit(listOf(resume))
        withTimeout(10_000.milliseconds) {
            var ready = states.awaitReady {
                it.resumes == listOf(resume) &&
                    it.vacancies == listOf(vacancy) &&
                    it.coverLetters == listOf(letter)
            }
            assertEquals(
                DashboardState.Ready(listOf(resume), listOf(vacancy), listOf(letter)),
                ready,
            )
            dependencies.vacanciesRepository.records.value = emptyList()
            ready = states.awaitReady { it.vacancies.isEmpty() }
            assertEquals(listOf(resume), ready.resumes)
            assertEquals(listOf(letter), ready.coverLetters)
            runOnUiThread { lifecycle.destroy() }
            cancellations.receive()
        }
        states.close()
        cancellations.close()
    }

    @Test
    fun failedReadCanBeRetriedAndReplacesPreviousObservation() = runBlocking<Unit> {
        var fail = true
        val repository = object : ResumesRepository by dependencies.resumesRepository {
            override fun observeResumes(projectId: Long): Flow<List<Resume>> = flow {
                if (fail) error("Cannot read records")
                emit(emptyList())
            }
        }
        val states = Channel<DashboardState>(Channel.UNLIMITED)
        lateinit var component: DefaultDashboardComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultDashboardComponent(context(repository), 7)
            component.uiState.subscribe { states.trySend(it) }
        }
        withTimeout(10_000) {
            while (states.receive() !=
                DashboardState.LoadFailed
            ) { /* Wait for the failed read. */ }
            runOnUiThread {
                fail = false
                component.onRetry()
                component.onRetry()
            }
            while (states.receive() !is DashboardState.Ready) { /* Wait for the retry. */ }
        }
        runOnUiThread { assertEquals(DashboardState.Ready(), component.uiState.value) }
        states.close()
    }

    private suspend fun Channel<DashboardState>.awaitReady(
        predicate: (DashboardState.Ready) -> Boolean,
    ): DashboardState.Ready {
        while (true) {
            val state = receive()
            if (state is DashboardState.Ready && predicate(state)) return state
        }
    }

    private fun context(repository: ResumesRepository) = DefaultAppComponentContext(
        DefaultComponentContext(lifecycle),
        object : ApplicationDependencies by dependencies {
            override val resumesRepository = repository
        },
    )
}
