package reframecv.dependencies

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.attachments.TestAttachmentStorage
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
import reframecv.database.project.ProjectEntity
import reframecv.domain.models.project.Project
import reframecv.repository.LocalProjectsRepository

class ApplicationDependenciesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun deletesProjectAndDescendantsAndPreservesOtherProjects() = runBlocking {
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
            listOf(root.id, child, grandchild, sibling).forEach {
                assertNull(dao.findById(it))
            }
            assertFailsWith<IllegalStateException> {
                repository.updateProject(child, "Removed child")
            }
            assertFailsWith<IllegalStateException> { repository.deleteProject(root.id) }
        } finally {
            database.close()
        }
        val reopened = buildDatabase(createDatabaseBuilder(file))
        try {
            assertNull(reopened.projectDao().findById(rootId))
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
        val dependencies =
            DefaultApplicationDependencies(attachmentStorage = TestAttachmentStorage()) {
                opened++
                buildDatabase(createDatabaseBuilder(file))
            }
        try {
            assertEquals(0, opened)
            assertSame(dependencies.storeFactory, dependencies.storeFactory)
            assertSame(dependencies.eventHistory, dependencies.eventHistory)
            val repository = dependencies.projectsRepository
            assertSame(repository, dependencies.projectsRepository)
            assertSame(dependencies.appSettingsRepository, dependencies.appSettingsRepository)
            assertSame(dependencies.resumesRepository, dependencies.resumesRepository)
            assertSame(dependencies.vacanciesRepository, dependencies.vacanciesRepository)
            assertSame(dependencies.coverLettersRepository, dependencies.coverLettersRepository)
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
        assertFailsWith<IllegalStateException> { dependencies.resumesRepository }
        assertFailsWith<IllegalStateException> { dependencies.vacanciesRepository }
        assertFailsWith<IllegalStateException> { dependencies.coverLettersRepository }
        assertFailsWith<IllegalStateException> { dependencies.appSettingsRepository }
        val reopened =
            DefaultApplicationDependencies(attachmentStorage = TestAttachmentStorage()) {
                buildDatabase(createDatabaseBuilder(file))
            }
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
        val dependencies =
            DefaultApplicationDependencies(attachmentStorage = TestAttachmentStorage()) {
                error("Should not open the database")
            }
        dependencies.close()
        dependencies.close()
        assertFailsWith<IllegalStateException> { dependencies.projectsRepository }
        assertFailsWith<IllegalStateException> { dependencies.resumesRepository }
        assertFailsWith<IllegalStateException> { dependencies.vacanciesRepository }
        assertFailsWith<IllegalStateException> { dependencies.coverLettersRepository }
        assertFailsWith<IllegalStateException> { dependencies.appSettingsRepository }
    }
}
