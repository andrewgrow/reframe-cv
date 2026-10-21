package reframecv.database.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.domain.models.project.ProjectMode
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.repository.LocalCoverLettersRepository
import reframecv.repository.LocalScreenStatesRepository

class CoverLetterBulkDeletionTest : WorkspaceDatabaseTest() {
    @Test
    fun deletionClearsVacancyLinksAndViewStatesButKeepsSourcesAndOtherLetters() =
        runBlocking<Unit> {
            val project = project()
            val dao = database.coverLetterDao()
            val resume = database.resumeDao().create(resume(project))
            val first = dao.create(letter(project).copy(sourceResumeId = resume))
            val second = dao.create(letter(project))
            val retained = dao.create(letter(project))
            val vacancy = database.vacancyDao().create(
                vacancy(project).copy(coverLetterIds = listOf(first, retained, second)),
            )
            val screens = LocalScreenStatesRepository(database.screenStatesDao())
            val owner = ScreenStateOwner(ScreenType.CoverLetter, first, project)
            screens.update(owner, buildJsonObject { put("details", "collapsed") })
            LocalCoverLettersRepository(dao).deleteMany(project, setOf(first, second))
            assertEquals(listOf(retained), dao.observeActive(project).first().map { it.entity.id })
            assertEquals(
                listOf(retained),
                database.vacancyDao().findById(vacancy)!!.toDomainModel().coverLetterIds,
            )
            assertNotNull(database.resumeDao().findById(resume))
            assertNull(screens.get(owner))
        }

    @Test
    fun foreignMissingAndEmptySelectionsLeaveAllLettersUntouched() = runBlocking<Unit> {
        val project = project()
        val other = project("Other")
        val repository = LocalCoverLettersRepository(database.coverLetterDao())
        val first = repository.create(letter(project))
        val foreign = repository.create(letter(other))
        assertFailsWith<IllegalArgumentException> {
            repository.deleteMany(project, setOf(first, foreign))
        }
        assertFailsWith<IllegalArgumentException> {
            repository.deleteMany(project, setOf(first, -1))
        }
        assertFailsWith<IllegalArgumentException> { repository.deleteMany(project, emptySet()) }
        assertEquals(listOf(first), repository.observeCoverLetters(project).first().map { it.id })
        assertEquals(listOf(foreign), repository.observeCoverLetters(other).first().map { it.id })
    }

    @Test
    fun recordsStayInIdOrderAfterRenamingAndDeletingLastLettersResetsWorkspace() =
        runBlocking<Unit> {
            val project = project()
            val repository = LocalCoverLettersRepository(database.coverLetterDao())
            val first = repository.create(letter(project).copy(name = "Z"))
            val second = repository.create(letter(project).copy(name = "A"))
            repository.update(
                repository.observeCoverLetters(project).first().first().copy(name = "New"),
            )
            assertEquals(
                listOf(first, second),
                repository.observeCoverLetters(project).first().map {
                    it.id
                },
            )
            repository.deleteMany(project, setOf(first, second))
            assertEquals(ProjectMode.Unconfigured, database.projectDao().findById(project)!!.mode)
        }
}
