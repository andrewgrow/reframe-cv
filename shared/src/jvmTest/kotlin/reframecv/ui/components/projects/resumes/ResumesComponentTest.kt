package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.resume.Resume
import reframecv.repository.ResumesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ObserveDashboardUseCase
import reframecv.usecase.ResumesUseCases

class ResumesComponentTest : ComponentTest() {
    @Test
    fun observesSelectedProjectAndCancelsOnDestroy() = runBlocking<Unit> {
        val records = MutableSharedFlow<List<Resume>>(replay = 1)
        val cancellations = Channel<Unit>(Channel.UNLIMITED)
        val repository = object : ResumesRepository by dependencies.resumesRepository {
            override fun observeResumes(projectId: Long): Flow<List<Resume>> {
                assertEquals(7L, projectId)
                return records.onCompletion { cancellations.trySend(Unit) }
            }
        }
        val states = Channel<ResumesState>(Channel.UNLIMITED)
        runOnUiThread {
            lifecycle.resume()
            val component = DefaultResumesComponent(context(repository), 7, back = {})
            component.uiState.subscribe { states.trySend(it) }
            assertIs<ResumesState.Loading>(component.uiState.value)
        }
        val resume = Resume(id = 2, projectId = 7, name = "Kotlin", createdAt = 1, updatedAt = 1)
        records.emit(listOf(resume))
        withTimeout(10_000.milliseconds) {
            assertEquals(
                ResumesState.Ready(listOf(resume)),
                states.awaitReady {
                    it.resumes.isNotEmpty()
                },
            )
            records.emit(emptyList())
            assertEquals(
                ResumesState.Ready(emptyList()),
                states.awaitReady {
                    it.resumes.isEmpty()
                },
            )
            runOnUiThread { lifecycle.destroy() }
            cancellations.receive()
            assertTrue(dependencies.logger.failures.tryReceive().isFailure)
        }
        states.close()
        cancellations.close()
    }

    @Test
    fun failedReadCanBeRetriedAndReplacesPreviousObservation() = runBlocking<Unit> {
        val failure = IllegalStateException("Cannot read records")
        var fail = true
        val repository = object : ResumesRepository by dependencies.resumesRepository {
            override fun observeResumes(projectId: Long): Flow<List<Resume>> = flow {
                if (fail) throw failure
                emit(emptyList())
            }
        }
        val states = Channel<ResumesState>(Channel.UNLIMITED)
        lateinit var component: DefaultResumesComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultResumesComponent(context(repository), 7, back = {})
            component.uiState.subscribe { states.trySend(it) }
        }
        withTimeout(10_000.milliseconds) {
            while (states.receive() !=
                ResumesState.LoadFailed
            ) { /* Wait for the failed read. */ }
            val logged = dependencies.logger.failures.receive()
            assertEquals("Load resumes", logged.operation)
            assertEquals(mapOf("projectId" to 7L), logged.context)
            // combine may wrap the exception when recovering its coroutine stack trace.
            assertSame(failure, logged.error.cause ?: logged.error)
            runOnUiThread {
                fail = false
                component.onRetry()
                component.onRetry()
            }
            while (states.receive() !is ResumesState.Ready) { /* Wait for the retry. */ }
        }
        runOnUiThread { assertEquals(ResumesState.Ready(emptyList()), component.uiState.value) }
        states.close()
    }

    private suspend fun Channel<ResumesState>.awaitReady(
        predicate: (ResumesState.Ready) -> Boolean,
    ): ResumesState.Ready {
        while (true) {
            val state = receive()
            if (state is ResumesState.Ready && predicate(state)) return state
        }
    }

    private fun context(repository: ResumesRepository) = DefaultAppComponentContext(
        DefaultComponentContext(lifecycle),
        object : ApplicationDependencies by dependencies {
            override val resumesUseCases get() = ResumesUseCases(resumesRepository)
            override val observeDashboard get() = ObserveDashboardUseCase(
                resumesRepository,
                vacanciesRepository,
                coverLettersRepository,
            )
            override val resumesRepository = repository
        },
    )
}
