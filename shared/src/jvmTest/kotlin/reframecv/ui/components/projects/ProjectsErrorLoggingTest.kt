package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.TestApplicationDependencies
import reframecv.domain.models.project.Project
import reframecv.logging.RecordingApplicationLogger
import reframecv.repository.ProjectsRepository
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ProjectsUseCases

class ProjectsErrorLoggingTest {
    @Test
    fun failuresRetainTheirExceptionAndIdentifyTheOperation() = runBlocking {
        val failure = IllegalStateException("Database unavailable")
        val delegate = TestApplicationDependencies().projectsRepository
        val repository = object : ProjectsRepository by delegate {
            override fun observeProjects(parentId: Long?) = flow<List<Project>> { throw failure }
            override suspend fun createProject(name: String, parentId: Long?): Project =
                throw failure
            override suspend fun updateProject(id: Long, name: String) = throw failure
            override suspend fun deleteProject(id: Long) = throw failure
        }
        val logger = RecordingApplicationLogger()
        val labels = Channel<ProjectsLabel>(Channel.UNLIMITED)
        val states = Channel<UiState>(Channel.UNLIMITED)
        val executor =
            RealProjectsExecutor(ProjectsUseCases(repository), parentId = 8L, logger = logger)
        try {
            runOnUiThread { executor.init(callbacks(states, labels)) }
            withTimeout(10.seconds) {
                runOnUiThread { executor.executeIntent(ProjectsIntent.LoadProjects) }
                assertEquals(UiState.Loading, states.receive())
                assertEquals(UiState.LoadFailed, states.receive())
                val load = logger.failures.receive()
                assertSame(failure, load.error)
                assertEquals("Load projects", load.operation)
                assertEquals(mapOf("parentId" to 8L), load.context)
                val intents = listOf(
                    ProjectsIntent.CreateProject("Backend") to "Create project",
                    ProjectsIntent.UpdateProject(42L, "Backend") to "Update project",
                    ProjectsIntent.DeleteProject(42L) to "Delete project",
                )
                for ((intent, operation) in intents) {
                    runOnUiThread { executor.executeIntent(intent) }
                    labels.receive()
                    assertEquals(
                        if (intent is ProjectsIntent.DeleteProject) {
                            ProjectsLabel.DeleteFailed
                        } else {
                            ProjectsLabel.SaveFailed
                        },
                        labels.receive(),
                    )
                    val logged = logger.failures.receive()
                    assertSame(failure, logged.error)
                    assertEquals(operation, logged.operation)
                    assertEquals(
                        if (intent is ProjectsIntent.CreateProject) {
                            mapOf("parentId" to 8L)
                        } else {
                            mapOf("projectId" to 42L)
                        },
                        logged.context,
                    )
                }
            }
        } finally {
            runOnUiThread { executor.dispose() }
        }
    }

    @Test
    fun cancellationIsNotReportedAsFailure() = runBlocking<Unit> {
        val delegate = TestApplicationDependencies().projectsRepository
        val repository = object : ProjectsRepository by delegate {
            override fun observeProjects(parentId: Long?) = flow<List<Project>> {
                throw CancellationException("Leaving screen")
            }
        }
        val logger = RecordingApplicationLogger()
        val states = Channel<UiState>(Channel.UNLIMITED)
        val executor = RealProjectsExecutor(ProjectsUseCases(repository), logger = logger)
        try {
            runOnUiThread {
                executor.init(callbacks(states, Channel(Channel.UNLIMITED)))
                executor.executeIntent(ProjectsIntent.LoadProjects)
            }
            runOnUiThread {
                assertTrue(logger.failures.tryReceive().isFailure)
                assertEquals(UiState.Loading, states.tryReceive().getOrThrow())
                assertTrue(states.tryReceive().isFailure)
            }
        } finally {
            runOnUiThread { executor.dispose() }
        }
    }

    private fun callbacks(states: Channel<UiState>, labels: Channel<ProjectsLabel>) =
        object : Executor.Callbacks<UiState, UiState, Nothing, ProjectsLabel> {
            override fun onAction(action: Nothing) = Unit
            override val state: UiState get() = UiState.Loading
            override fun onMessage(message: UiState) {
                states.trySend(message)
            }
            override fun onLabel(label: ProjectsLabel) {
                labels.trySend(label)
            }
        }
}
