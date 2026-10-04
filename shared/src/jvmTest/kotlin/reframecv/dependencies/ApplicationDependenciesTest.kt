package reframecv.dependencies

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
import reframecv.database.project.ProjectEntity
import reframecv.domain.models.project.Project
import reframecv.repository.LocalProjectsRepository

class ApplicationDependenciesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun marksProjectAndDescendantsDeletedAndPreservesOtherProjects() = runBlocking {
        val file = temporaryFolder.root.toPath().resolve("delete.db")
        val database = buildDatabase(createDatabaseBuilder(file))
        var remainingId = 0L
        var rootId = 0L
        try {
            val dao = database.projectDao()
            val repository = LocalProjectsRepository(dao)
            val root = repository.createProject("Root")
            rootId = root.id
            remainingId = repository.createProject("Unrelated").id
            val child = dao.insert(
                ProjectEntity(name = "Child", createdAt = 1, updatedAt = 1, parentId = root.id),
            )
            val grandchild = dao.insert(
                ProjectEntity(name = "Grandchild", createdAt = 1, updatedAt = 1, parentId = child),
            )
            val sibling = dao.insert(
                ProjectEntity(name = "Sibling", createdAt = 1, updatedAt = 1, parentId = root.id),
            )
            assertFailsWith<IllegalStateException> { repository.deleteProject(-1) }
            assertEquals(2, repository.observeProjects().first().size)

            repository.deleteProject(root.id)
            assertEquals(remainingId, repository.observeProjects().first().single().id)
            val deletedAt = assertNotNull(dao.findById(root.id)?.deletedAt)
            assertTrue(deletedAt > 0)
            listOf(child, grandchild, sibling).forEach {
                assertEquals(deletedAt, dao.findById(it)?.deletedAt)
            }
            repository.updateProject(child, "Renamed deleted child")
            assertEquals("Renamed deleted child", dao.findById(child)?.name)
            assertEquals(deletedAt, dao.findById(child)?.deletedAt)
            assertEquals(remainingId, repository.observeProjects().first().single().id)
        } finally {
            database.close()
        }
        val reopened = buildDatabase(createDatabaseBuilder(file))
        try {
            assertNotNull(reopened.projectDao().findById(rootId)?.deletedAt)
            assertEquals("Root", reopened.projectDao().findById(rootId)?.name)
            assertEquals(remainingId, reopened.projectDao().observeAll().first().single().id)
            LocalProjectsRepository(reopened.projectDao()).deleteProject(remainingId)
            assertEquals(emptyList(), reopened.projectDao().observeAll().first())
        } finally {
            reopened.close()
        }
    }

    @Test
    fun updatesOnlyNameAndTimestampAndRejectsBlankOrMissingProject() = runBlocking {
        val file = temporaryFolder.root.toPath().resolve("update.db")
        val database = buildDatabase(createDatabaseBuilder(file))
        try {
            val dao = database.projectDao()
            val repository = LocalProjectsRepository(dao)
            val parent = repository.createProject("Parent")
            val oldTime = 1_000L
            val child = Project(
                name = "Mobile",
                createdAt = oldTime,
                updatedAt = oldTime,
                parentId = parent.id,
            )
            val id = dao.insert(ProjectEntity.fromDomainModel(child))
            assertFailsWith<IllegalArgumentException> { repository.updateProject(id, "  ") }
            assertFailsWith<IllegalStateException> { repository.updateProject(-1, "Missing") }
            assertEquals(child.copy(id = id), dao.findById(id)?.toDomainModel())

            repository.updateProject(id, "  Android Developer  ")
            val updated = checkNotNull(dao.findById(id)).toDomainModel()
            assertEquals("Android Developer", updated.name)
            assertEquals(oldTime, updated.createdAt)
            assertEquals(parent.id, updated.parentId)
            assertTrue(updated.updatedAt > oldTime)
            assertEquals(1, repository.observeProjects().first().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun reusesRepositoryAndDatabaseAndPersistsProjects() = runBlocking {
        var opened = 0
        val file = temporaryFolder.root.toPath().resolve("projects.db")
        val dependencies = DefaultApplicationDependencies {
            opened++
            buildDatabase(createDatabaseBuilder(file))
        }
        try {
            assertEquals(0, opened)
            val repository = dependencies.projectsRepository
            assertSame(repository, dependencies.projectsRepository)
            assertEquals(1, opened)
            assertFailsWith<IllegalArgumentException> { repository.createProject("  ") }
            val project = repository.createProject("  Android Developer  ")
            assertEquals("Android Developer", project.name)
            assertEquals(listOf(project), repository.observeProjects().first())
        } finally {
            dependencies.close()
        }
        dependencies.close()
        assertFailsWith<IllegalStateException> { dependencies.projectsRepository }
        val reopened = DefaultApplicationDependencies { buildDatabase(createDatabaseBuilder(file)) }
        try {
            assertEquals(
                "Android Developer",
                reopened.projectsRepository.observeProjects().first().single().name,
            )
        } finally {
            reopened.close()
        }
    }

    @Test
    fun closingUnusedDependenciesDoesNotOpenDatabase() {
        val dependencies = DefaultApplicationDependencies { error("Should not open the database") }
        dependencies.close()
        dependencies.close()
        assertFailsWith<IllegalStateException> { dependencies.projectsRepository }
    }
}
