package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.core.store.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
import reframecv.repository.LocalProjectsRepository
import reframecv.ui.threading.runOnUiThread

private typealias ProjectsCallbacks = Executor.Callbacks<UiState, UiState, Nothing, ProjectsLabel>

class RealProjectsExecutorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun savesTrimmedNamesAndObservesPersistedProjects() = runBlocking {
        val file = temporaryFolder.root.toPath().resolve("projects.db")
        val database = buildDatabase(createDatabaseBuilder(file))
        val messages = Channel<UiState>(Channel.UNLIMITED)
        val labels = Channel<ProjectsLabel>(Channel.UNLIMITED)
        val executor = RealProjectsExecutor(LocalProjectsRepository(database.projectDao()))
        try {
            runOnUiThread {
                executor.init(object : ProjectsCallbacks {
                    override val state: UiState = UiState.NoProjects
                    override fun onMessage(message: UiState) {
                        messages.trySend(message)
                    }
                    override fun onLabel(label: ProjectsLabel) {
                        labels.trySend(label)
                    }
                    override fun onAction(action: Nothing) = Unit
                })
                executor.executeIntent(ProjectsIntent.LoadProjects)
            }
            withTimeout(10_000L) {
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
            }
        } finally {
            runOnUiThread { executor.dispose() }
            database.close()
        }
        val reopened = buildDatabase(createDatabaseBuilder(file))
        try {
            assertEquals(
                "O'Reilly; DROP TABLE projects; --",
                reopened.projectDao().observeAll().first().single().name,
            )
        } finally {
            reopened.close()
        }
    }
}
