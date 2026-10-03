package reframecv.dependencies

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder

class ApplicationDependenciesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

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
