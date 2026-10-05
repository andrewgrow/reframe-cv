package reframecv.database.workspace

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.database.AppDatabase
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
import reframecv.database.project.ProjectEntity
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy

abstract class WorkspaceDatabaseTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()
    protected lateinit var database: AppDatabase

    @Before
    fun openDatabase() {
        database =
            buildDatabase(
                createDatabaseBuilder(temporaryFolder.root.toPath().resolve("workspace.db")),
            )
    }

    @After
    fun closeDatabase() = database.close()

    protected suspend fun project(name: String = "Android Developer"): Long =
        database.projectDao().insert(
            ProjectEntity(name = name, createdAt = 100, updatedAt = 100),
        )

    protected fun resume(projectId: Long) = Resume(
        projectId = projectId,
        name = "Resume",
        createdAt = 200,
        updatedAt = 200,
        content = "Experience",
        keywords = listOf(" Kotlin ", "kotlin", "", " Android ", "   ", "Résumé"),
    )

    protected fun letter(projectId: Long) = CoverLetter(
        projectId = projectId,
        name = "Letter",
        createdAt = 200,
        updatedAt = 200,
        content = "Dear team",
        keywords = listOf(" Kotlin ", "kotlin", "", " Android ", "   ", "Résumé"),
    )

    protected fun vacancy(projectId: Long) = Vacancy(
        projectId = projectId,
        name = "Vacancy",
        createdAt = 200,
        updatedAt = 200,
        description = "Job description",
        company = "Company",
        url = "https://example.com/job",
        keywords = listOf(" Kotlin ", "kotlin", "", " Android ", "   ", "Résumé"),
    )
}
