package reframecv.database

import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
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
        createdAtEpochMilliseconds = 500L,
        updatedAtEpochMilliseconds = updatedAt,
    )

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
        val timestamp = Instant.fromEpochMilliseconds(1_000L)
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
