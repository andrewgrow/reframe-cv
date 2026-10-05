package reframecv.repository

import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.database.AppDatabase
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
import reframecv.database.project.ProjectEntity
import reframecv.domain.models.project.ProjectMode

class LocalProjectsRepositoryModeTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var repository: LocalProjectsRepository

    @Before
    fun setUp() {
        database =
            buildDatabase(createDatabaseBuilder(temporaryFolder.root.toPath().resolve("modes.db")))
        repository = LocalProjectsRepository(database.projectDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun observesAllActiveProjectsAcrossLevels() = runBlocking {
        val parent = repository.createProject("Backend")
        val child = repository.createProject("Java", parent.id)
        val sibling = repository.createProject("Mobile")
        assertEquals(
            setOf(parent.id, child.id, sibling.id),
            repository.observeAllProjects().first().map { it.id }.toSet(),
        )
        repository.updateProject(child.id, "Java Developer")
        assertEquals(
            "Java Developer",
            repository.observeAllProjects().first().single { it.id == child.id }.name,
        )
        repository.deleteProject(parent.id)
        assertEquals(listOf(sibling.id), repository.observeAllProjects().first().map { it.id })
    }

    @Test
    fun commitsContainerModeOnlyAfterCreatingAChild() = runBlocking {
        val parent = repository.createProject("Backend")
        assertEquals(ProjectMode.Unconfigured, parent.mode)
        assertFailsWith<IllegalArgumentException> { repository.createProject("  ", parent.id) }
        assertEquals(parent, database.projectDao().findById(parent.id)?.toDomainModel())

        val child = repository.createProject("Java", parent.id)
        val updatedParent = assertNotNull(database.projectDao().findById(parent.id)).toDomainModel()
        assertEquals(ProjectMode.Container, updatedParent.mode)
        assertEquals(parent.createdAt, updatedParent.createdAt)
        assertEquals(parent.id, child.parentId)
        assertEquals(ProjectMode.Unconfigured, child.mode)
        repository.createProject("Node.js", parent.id)
        assertEquals(2, repository.observeProjects(parent.id).first().size)
        assertEquals(ProjectMode.Container, repository.observeProjects().first().single().mode)
        val grandchild = repository.createProject("Applications", child.id)
        assertEquals(ProjectMode.Container, database.projectDao().findById(child.id)?.mode)
        assertEquals(ProjectMode.Unconfigured, grandchild.mode)
    }

    @Test
    fun rejectsChildrenUnderWorkspaceDeletedAndMissingParents() = runBlocking {
        val dao = database.projectDao()
        val workspace = ProjectEntity(
            name = "Workspace",
            createdAt = 1,
            updatedAt = 1,
            mode = ProjectMode.Workspace,
        )
        val workspaceId = dao.insert(workspace)
        assertFailsWith<IllegalStateException> { repository.createProject("Child", workspaceId) }
        assertEquals(workspace.copy(id = workspaceId), dao.findById(workspaceId))
        assertEquals(emptyList(), repository.observeProjects(workspaceId).first())
        val deleted = repository.createProject("Deleted")
        repository.deleteProject(deleted.id)
        assertFailsWith<IllegalStateException> { repository.createProject("Child", deleted.id) }
        assertFailsWith<IllegalStateException> { repository.createProject("Child", -1) }
        assertEquals(ProjectMode.Unconfigured, dao.findById(deleted.id)?.mode)
        assertEquals(emptyList(), repository.observeProjects(deleted.id).first())
    }

    @Test
    fun failedInsertRollsBackParentModeAndTimestamp() = runBlocking {
        val parent = repository.createProject("Backend")
        val conflict = repository.createProject("Existing")
        assertFailsWith<SQLiteException> {
            database.projectDao().insertWithParentMode(
                ProjectEntity(
                    id = conflict.id,
                    name = "Conflicting child",
                    createdAt = parent.createdAt + 1,
                    updatedAt = parent.updatedAt + 1,
                    parentId = parent.id,
                ),
            )
        }
        assertEquals(parent, database.projectDao().findById(parent.id)?.toDomainModel())
        assertNull(database.projectDao().findById(conflict.id)?.parentId)
        assertEquals(emptyList(), repository.observeProjects(parent.id).first())
    }

    @Test
    fun allModesRoundTripAndPersistAfterReopening() = runBlocking {
        val dao = database.projectDao()
        val projects = ProjectMode.entries.map { mode ->
            val project = ProjectEntity(name = mode.name, createdAt = 1, updatedAt = 1, mode = mode)
            project.copy(id = dao.insert(project))
        }
        projects.forEach { project ->
            assertEquals(project, ProjectEntity.fromDomainModel(project.toDomainModel()))
        }
        database.close()
        database =
            buildDatabase(createDatabaseBuilder(temporaryFolder.root.toPath().resolve("modes.db")))
        projects.forEach { project ->
            assertEquals(project, database.projectDao().findById(project.id))
        }
    }
}
