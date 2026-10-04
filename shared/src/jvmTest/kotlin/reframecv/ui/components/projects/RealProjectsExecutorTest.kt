package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Executor
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
import reframecv.domain.models.project.Project
import reframecv.repository.LocalProjectsRepository
import reframecv.repository.ProjectsRepository
import reframecv.ui.threading.runOnUiThread

private typealias ProjectsCallbacks = Executor.Callbacks<UiState, UiState, Nothing, ProjectsLabel>

class RealProjectsExecutorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun remainsLoadingUntilRepositoryReturnsItsFirstResult() = runBlocking {
        val result = CompletableDeferred<List<Project>>()
        val repository = object : ProjectsRepository {
            override fun observeProjects(parentId: Long?) = flow { emit(result.await()) }
            override suspend fun createProject(name: String, parentId: Long?): Project =
                error("Unexpected create")
            override suspend fun updateProject(id: Long, name: String) = error("Unexpected update")
            override suspend fun deleteProject(id: Long) = error("Unexpected delete")
        }
        val messages = Channel<UiState>(Channel.UNLIMITED)
        val labels = Channel<ProjectsLabel>(Channel.UNLIMITED)
        val executor = RealProjectsExecutor(repository)
        try {
            runOnUiThread {
                executor.init(callbacks(messages, labels))
                executor.executeIntent(ProjectsIntent.LoadProjects)
            }
            withTimeout(10_000L.milliseconds) {
                assertEquals(UiState.Loading, messages.receive())
                assertTrue(messages.tryReceive().isFailure)
                result.complete(emptyList())
                assertEquals(UiState.NoProjects, messages.receive())
                runOnUiThread { executor.executeIntent(ProjectsIntent.LoadProjects) }
                assertEquals(UiState.Loading, messages.receive())
                assertEquals(UiState.NoProjects, messages.receive())
            }
        } finally {
            runOnUiThread { executor.dispose() }
        }
    }

    @Test
    fun observesOnlyDirectChildrenAndCreatesUnderCurrentParent() = runBlocking {
        val database =
            buildDatabase(
                createDatabaseBuilder(temporaryFolder.root.toPath().resolve("children.db")),
            )
        val repository = LocalProjectsRepository(database.projectDao())
        val parent = repository.createProject("Backend")
        val other = repository.createProject("Mobile")
        val sibling = repository.createProject("Sibling", other.id)
        val messages = Channel<UiState>(Channel.UNLIMITED)
        val labels = Channel<ProjectsLabel>(Channel.UNLIMITED)
        val executor = RealProjectsExecutor(repository, parent.id)
        try {
            runOnUiThread {
                executor.init(callbacks(messages, labels))
                executor.executeIntent(ProjectsIntent.LoadProjects)
            }
            withTimeout(10_000L.milliseconds) {
                assertEquals(UiState.Loading, messages.receive())
                assertEquals(UiState.NoProjects, messages.receive())
                runOnUiThread { executor.executeIntent(ProjectsIntent.CreateProject("  Google  ")) }
                assertEquals(ProjectsLabel.Saving, labels.receive())
                assertEquals(ProjectsLabel.Saved, labels.receive())
                val child = (messages.receive() as UiState.Projects).projects.single()
                assertEquals(parent.id, child.parentId)
                assertEquals("Google", child.name)
                val grandchild = repository.createProject("Resume", child.id)
                assertEquals(
                    setOf(parent.id, other.id),
                    repository.observeProjects().first().map {
                        it.id
                    }.toSet(),
                )
                assertEquals(listOf(child), repository.observeProjects(parent.id).first())
                assertEquals(listOf(grandchild), repository.observeProjects(child.id).first())
                assertEquals(listOf(sibling), repository.observeProjects(other.id).first())
                repository.deleteProject(child.id)
                assertEquals(emptyList(), repository.observeProjects(parent.id).first())
                assertEquals(emptyList(), repository.observeProjects(child.id).first())
            }
        } finally {
            runOnUiThread { executor.dispose() }
            database.close()
        }
    }

    @Test
    fun deletesProjectAndReportsMissingProjectFailure() = runBlocking {
        val database =
            buildDatabase(createDatabaseBuilder(temporaryFolder.root.toPath().resolve("delete.db")))
        val repository = LocalProjectsRepository(database.projectDao())
        val project = repository.createProject("Android Developer")
        val messages = Channel<UiState>(Channel.UNLIMITED)
        val labels = Channel<ProjectsLabel>(Channel.UNLIMITED)
        val executor = RealProjectsExecutor(repository)
        try {
            runOnUiThread {
                executor.init(callbacks(messages, labels))
                executor.executeIntent(ProjectsIntent.LoadProjects)
            }
            withTimeout(10_000L.milliseconds) {
                assertEquals(UiState.Loading, messages.receive())
                assertEquals(listOf(project), (messages.receive() as UiState.Projects).projects)
                runOnUiThread { executor.executeIntent(ProjectsIntent.DeleteProject(project.id)) }
                assertEquals(ProjectsLabel.Deleting, labels.receive())
                assertEquals(ProjectsLabel.Deleted, labels.receive())
                assertEquals(UiState.NoProjects, messages.receive())
                runOnUiThread { executor.executeIntent(ProjectsIntent.DeleteProject(project.id)) }
                assertEquals(ProjectsLabel.Deleting, labels.receive())
                assertEquals(ProjectsLabel.DeleteFailed, labels.receive())
            }
        } finally {
            runOnUiThread { executor.dispose() }
            database.close()
        }
    }

    @Test
    fun reportsLoadFailureWhenRepositoryObservationFails() = runBlocking {
        val repository = object : ProjectsRepository {
            override suspend fun deleteProject(id: Long) = error("Unexpected delete")
            override fun observeProjects(parentId: Long?) = flow<List<Project>> {
                throw IllegalStateException("Database read failed")
            }
            override suspend fun createProject(name: String, parentId: Long?): Project =
                error("Unexpected create")
            override suspend fun updateProject(id: Long, name: String) = error("Unexpected update")
        }
        val messages = Channel<UiState>(Channel.UNLIMITED)
        val labels = Channel<ProjectsLabel>(Channel.UNLIMITED)
        val executor = RealProjectsExecutor(repository)
        try {
            runOnUiThread {
                executor.init(callbacks(messages, labels))
                executor.executeIntent(ProjectsIntent.LoadProjects)
            }
            withTimeout(10_000L.milliseconds) {
                assertEquals(UiState.Loading, messages.receive())
                assertEquals(UiState.LoadFailed, messages.receive())
            }
        } finally {
            runOnUiThread { executor.dispose() }
        }
    }

    @Test
    fun savesTrimmedNamesAndObservesPersistedProjects() = runBlocking {
        val file = temporaryFolder.root.toPath().resolve("projects.db")
        val database = buildDatabase(createDatabaseBuilder(file))
        val messages = Channel<UiState>(Channel.UNLIMITED)
        val labels = Channel<ProjectsLabel>(Channel.UNLIMITED)
        val executor = RealProjectsExecutor(LocalProjectsRepository(database.projectDao()))
        try {
            runOnUiThread {
                executor.init(callbacks(messages, labels))
                executor.executeIntent(ProjectsIntent.LoadProjects)
            }
            withTimeout(10_000L.milliseconds) {
                assertEquals(UiState.Loading, messages.receive())
                assertEquals(UiState.NoProjects, messages.receive())
                runOnUiThread { executor.executeIntent(ProjectsIntent.CreateProject("   ")) }
                assertEquals(emptyList(), database.projectDao().observeAll().first())
                val name = "O'Reilly; DROP TABLE projects; --"
                runOnUiThread { executor.executeIntent(ProjectsIntent.CreateProject("  $name  ")) }
                assertEquals(ProjectsLabel.Saving, labels.receive())
                assertEquals(ProjectsLabel.Saved, labels.receive())
                val state = messages.receive() as UiState.Projects
                assertEquals(name, state.projects.single().name)
                assertNull(state.projects.single().parentId)
                assertEquals(
                    state.projects.single(),
                    database.projectDao()
                        .findById(state.projects.single().id)?.toDomainModel(),
                )
                val original = state.projects.single()
                runOnUiThread {
                    executor.executeIntent(ProjectsIntent.UpdateProject(original.id, "   "))
                }
                assertEquals(original, database.projectDao().findById(original.id)?.toDomainModel())
                runOnUiThread {
                    executor.executeIntent(ProjectsIntent.UpdateProject(original.id, "  Backend  "))
                }
                assertEquals(ProjectsLabel.Saving, labels.receive())
                assertEquals(ProjectsLabel.Saved, labels.receive())
                val updated = (messages.receive() as UiState.Projects).projects.single()
                assertEquals("Backend", updated.name)
                assertEquals(original.id, updated.id)
                assertEquals(original.createdAt, updated.createdAt)
                assertEquals(original.parentId, updated.parentId)
                assertEquals(updated, database.projectDao().findById(original.id)?.toDomainModel())
                runOnUiThread {
                    executor.executeIntent(ProjectsIntent.UpdateProject(-1, "Missing"))
                }
                assertEquals(ProjectsLabel.Saving, labels.receive())
                assertEquals(ProjectsLabel.SaveFailed, labels.receive())
            }
        } finally {
            runOnUiThread { executor.dispose() }
            database.close()
        }
        assertPersistedName(file, "Backend")
    }

    private suspend fun assertPersistedName(file: Path, expectedName: String) {
        val reopened = buildDatabase(createDatabaseBuilder(file))
        try {
            assertEquals(
                expectedName,
                reopened.projectDao().observeAll().first().single().name,
            )
        } finally {
            reopened.close()
        }
    }

    private fun callbacks(messages: Channel<UiState>, labels: Channel<ProjectsLabel>) =
        object : ProjectsCallbacks {
            override val state: UiState = UiState.NoProjects
            override fun onMessage(message: UiState) {
                messages.trySend(message)
            }
            override fun onLabel(label: ProjectsLabel) {
                labels.trySend(label)
            }
            override fun onAction(action: Nothing) = Unit
        }
}
