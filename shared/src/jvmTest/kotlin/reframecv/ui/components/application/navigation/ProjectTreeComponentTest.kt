package reframecv.ui.components.application.navigation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.dependencies.TestApplicationDependencies
import reframecv.domain.models.project.Project
import reframecv.logging.EventHistory
import reframecv.repository.ProjectsRepository
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ProjectsUseCases

class ProjectTreeComponentTest {
    @Test
    fun retriesFailedObservationAndCancelsItWithLifecycle() = runBlocking {
        val dependencies = TestApplicationDependencies()
        val lifecycle = LifecycleRegistry()
        val cancelled = CompletableDeferred<Unit>()
        val projects = listOf(
            Project(1, "Backend", 0, 0),
            Project(2, "Java", 0, 0, parentId = 1),
        )
        var attempts = 0
        val repository = object : ProjectsRepository by dependencies.projectsRepository {
            override fun observeAllProjects() = flow {
                attempts++
                if (attempts == 1) error("Read failed")
                try {
                    emit(projects)
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
        }
        val customDependencies = object : ApplicationDependencies by dependencies {
            override val projectsUseCases get() = ProjectsUseCases(projectsRepository)
            override val projectsRepository = repository
        }
        val states = Channel<ProjectTreeState>(Channel.UNLIMITED)
        lateinit var component: DefaultProjectTreeComponent
        val opened = mutableListOf<List<Project>>()
        runOnUiThread {
            component = DefaultProjectTreeComponent(
                DefaultAppComponentContext(DefaultComponentContext(lifecycle), customDependencies),
                openProject = { opened += it },
                openProjectsList = {},
                onProjectsChanged = {},
            )
            lifecycle.resume()
            component.state.subscribe { states.trySend(it) }
        }
        try {
            withTimeout(10_000L.milliseconds) {
                assertTrue(states.receive().loading)
                assertTrue(states.receive().failed)
                assertLoadFailure(dependencies)
                runOnUiThread {
                    component.select(2)
                    component.onRetry()
                }
                var state: ProjectTreeState
                do {
                    state = states.receive()
                } while (state.loading || state.failed)
                assertFalse(state.failed)
                assertEquals(setOf(1L), state.expandedIds)
                runOnUiThread {
                    component.onProjectSelected(99)
                    assertTrue(opened.isEmpty())
                    component.onToggle(1)
                    component.onProjectSelected(2)
                    component.onProjectsList()
                    assertEquals(listOf(projects), opened)
                    assertNavigationHistory(dependencies.eventHistory)
                }
            }
        } finally {
            runOnUiThread { lifecycle.destroy() }
        }
        withTimeout(10_000L.milliseconds) { cancelled.await() }
        assertEquals(2, attempts)
    }

    private suspend fun assertLoadFailure(dependencies: TestApplicationDependencies) {
        assertTrue(dependencies.eventHistory.snapshot().last().endsWith("LoadFailed"))
        val logged = dependencies.logger.failures.receive()
        assertEquals("Load project tree", logged.operation)
        assertTrue(logged.context.isEmpty())
        assertEquals("Read failed", logged.error.message)
    }

    private fun assertNavigationHistory(events: EventHistory) {
        val history = events.snapshot().joinToString("\n")
        assertTrue(history.contains("Selected(projectId=2)"))
        assertTrue(history.contains("Toggle(projectId=1, expanded=false)"))
        assertTrue(history.contains("OpenProject(projectId=2, path=[1, 2])"))
        assertTrue(history.contains("OpenProjectsList"))
        assertFalse(history.contains("Backend"))
        assertFalse(history.contains("Java"))
    }
}
