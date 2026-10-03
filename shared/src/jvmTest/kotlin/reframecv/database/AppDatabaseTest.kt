package reframecv.database

import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.database.project.ProjectEntity
import reframecv.domain.models.project.Project

private const val PROJECT_NAME = "Test Project"
private const val SECOND_PROJECT_NAME = "Second Test Project"
private const val UPDATED_PROJECT_NAME = "Updated Test Project"

class AppDatabaseTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database =
            buildDatabase(createDatabaseBuilder(temporaryFolder.root.toPath().resolve("test.db")))
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    private fun entity(name: String = PROJECT_NAME, updatedAt: Long = 1_000L) = ProjectEntity(
        name = name,
        createdAt = 500L,
        updatedAt = updatedAt,
    )

    @Test
    fun marksSubtreeWithOneTimestampAndPreservesEarlierDeletionTimes() = runBlocking {
        val dao = database.projectDao()
        val root = dao.insert(entity("Root"))
        val child = dao.insert(entity("Already deleted").copy(parentId = root, deletedAt = 100L))
        val grandchild = dao.insert(entity("Grandchild").copy(parentId = child))
        val unrelated = dao.insert(entity("Unrelated"))

        assertEquals(2, dao.markSubtreeDeleted(root, 300L))
        assertEquals(300L, dao.findById(root)?.deletedAt)
        assertEquals(100L, dao.findById(child)?.deletedAt)
        assertEquals(300L, dao.findById(grandchild)?.deletedAt)
        assertNull(dao.findById(unrelated)?.deletedAt)
        assertEquals(listOf(unrelated), dao.observeAll().first().map { it.id })
        assertEquals(0, dao.markSubtreeDeleted(root, 400L))
        assertEquals(0, dao.markSubtreeDeleted(-1, 400L))
        assertEquals(300L, dao.findById(root)?.deletedAt)
        assertEquals(100L, dao.findById(child)?.deletedAt)
        val stored = requireNotNull(dao.findById(grandchild))
        assertEquals(stored, ProjectEntity.fromDomainModel(stored.toDomainModel()))
    }

    @Test
    fun storesProjectHierarchyAndMapsParentId() = runBlocking {
        val dao = database.projectDao()
        val rootId = dao.insert(entity())
        val child = entity("Child").copy(parentId = rootId)
        val childId = dao.insert(child)
        val siblingId = dao.insert(entity("Sibling").copy(parentId = rootId))
        val grandchild = entity("Grandchild").copy(parentId = childId)
        val grandchildId = dao.insert(grandchild)

        assertNull(dao.findById(rootId)?.parentId)
        assertEquals(rootId, dao.findById(childId)?.parentId)
        assertEquals(rootId, dao.findById(siblingId)?.parentId)
        assertEquals(childId, dao.findById(grandchildId)?.parentId)
        val storedChild = requireNotNull(dao.findById(childId))
        assertEquals(rootId, storedChild.toDomainModel().parentId)
        assertEquals(storedChild, ProjectEntity.fromDomainModel(storedChild.toDomainModel()))
    }

    @Test
    fun rejectsMissingParentAndDeletingParentWithChildren() = runBlocking {
        val dao = database.projectDao()
        assertFailsWith<SQLiteException> {
            dao.insert(entity().copy(parentId = 99L))
        }
        val root = entity()
        val rootId = dao.insert(root)
        val child = entity("Child").copy(parentId = rootId)
        val childId = dao.insert(child)

        assertFailsWith<SQLiteException> { dao.delete(root.copy(id = rootId)) }
        assertEquals(rootId, dao.findById(childId)?.parentId)
        dao.delete(child.copy(id = childId))
        dao.delete(root.copy(id = rootId))
        assertNull(dao.findById(rootId))
    }

    @Test
    fun updatesDeletesAndReturnsNullForMissingProject() = runBlocking {
        val dao = database.projectDao()
        assertNull(dao.findById(99L))
        val firstId = dao.insert(entity())
        val secondId = dao.insert(entity(SECOND_PROJECT_NAME))
        assertNotEquals(firstId, secondId)
        val updated = entity(UPDATED_PROJECT_NAME, 2_000L).copy(id = firstId)
        dao.update(updated)
        assertEquals(updated, dao.findById(firstId))
        dao.delete(updated)
        assertNull(dao.findById(firstId))
        assertEquals(secondId, dao.findById(secondId)?.id)
    }

    @Test
    fun sortsByUpdatedTimeThenNameAndRejectsDuplicateIds() = runBlocking {
        val dao = database.projectDao()
        val older = entity("Older", 100L)
        val zulu = entity("Zulu", 200L)
        val alpha = entity("Alpha", 200L)
        dao.insert(older)
        dao.insert(zulu)
        val id = dao.insert(alpha)
        assertEquals(listOf("Alpha", "Zulu", "Older"), dao.observeAll().first().map { it.name })
        assertFailsWith<SQLiteException> {
            dao.insert(entity("Conflict").copy(id = id))
        }
        assertEquals(alpha.copy(id = id), dao.findById(id))
    }

    @Test
    fun flowEmitsAfterInsertUpdateAndDelete() = runBlocking {
        val values = Channel<List<ProjectEntity>>(Channel.UNLIMITED)
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            database.projectDao().observeAll().collect { values.send(it) }
        }
        try {
            withTimeout(10_000.milliseconds) {
                assertEquals(emptyList(), values.receive())
                val dao = database.projectDao()
                val project = entity()
                val stored = project.copy(id = dao.insert(project))
                assertEquals(listOf(stored), values.receive())
                val updated = stored.copy(name = UPDATED_PROJECT_NAME)
                dao.update(updated)
                assertEquals(listOf(updated), values.receive())
                dao.delete(updated)
                assertEquals(emptyList(), values.receive())
            }
        } finally {
            observer.cancel()
            observer.join()
            values.close()
        }
    }

    @Test
    fun persistsProjectsAfterReopeningDatabase() = runBlocking {
        val file = temporaryFolder.root.toPath().resolve("nested/project/test.db")
        val firstDatabase = buildDatabase(createDatabaseBuilder(file))
        val project = entity()
        val id = try {
            firstDatabase.projectDao().insert(project)
        } finally {
            firstDatabase.close()
        }
        val reopened = buildDatabase(createDatabaseBuilder(file))
        try {
            assertEquals(project.copy(id = id), reopened.projectDao().findById(id))
        } finally {
            reopened.close()
        }
    }

    @Test
    fun storesAndObservesProjects() = runBlocking {
        val timestamp = 1_000L
        val project = Project(
            name = PROJECT_NAME,
            createdAt = timestamp,
            updatedAt = timestamp,
        )

        val projectId = database.projectDao().insert(ProjectEntity.fromDomainModel(project))
        val storedProject = project.copy(id = projectId)

        assertEquals(1L, projectId)
        assertEquals(storedProject, database.projectDao().findById(projectId)?.toDomainModel())
        assertEquals(
            listOf(storedProject),
            database.projectDao().observeAll().first().map(ProjectEntity::toDomainModel),
        )
    }
}
