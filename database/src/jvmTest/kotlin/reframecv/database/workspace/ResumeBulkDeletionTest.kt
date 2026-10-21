package reframecv.database.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.repository.LocalResumesRepository
import reframecv.repository.LocalScreenStatesRepository

class ResumeBulkDeletionTest : WorkspaceDatabaseTest() {
    @Test
    fun bulkDeletionClearsReferencesMainSelectionAndScreenStatesButKeepsVacanciesAndLetters() =
        runBlocking<Unit> {
            val project = project()
            val dao = database.resumeDao()
            val main = dao.create(resume(project))
            val second = dao.create(resume(project))
            val retained = dao.create(resume(project).copy(sourceResumeId = main))
            database.projectDao().setMainResume(project, main, 300)
            val letter = database.coverLetterDao().create(
                letter(project).copy(sourceResumeId = main),
            )
            val vacancy = database.vacancyDao().create(
                vacancy(
                    project,
                ).copy(
                    resumes = listOf(
                        VacancyResume(main),
                        VacancyResume(second),
                        VacancyResume(retained),
                    ),
                ),
            )
            val owner = ScreenStateOwner(ScreenType.Resume, main, project)
            val screens = LocalScreenStatesRepository(database.screenStatesDao())
            screens.update(owner, buildJsonObject { put("details", "collapsed") })
            LocalResumesRepository(dao).deleteMany(project, setOf(main, second))
            assertEquals(listOf(retained), dao.observeActive(project).first().map { it.entity.id })
            assertNull(database.projectDao().findById(project)!!.mainResumeId)
            assertNull(dao.findById(retained)!!.entity.sourceResumeId)
            assertNull(database.coverLetterDao().findById(letter)!!.entity.sourceResumeId)
            assertEquals(
                listOf(VacancyResume(retained)),
                database.vacancyDao().findById(vacancy)!!.toDomainModel().resumes,
            )
            assertNull(screens.get(owner))
        }

    @Test
    fun foreignAndMissingIdsCannotPartiallyDeleteSelectedRecords() = runBlocking<Unit> {
        val project = project()
        val other = project("Other")
        val a = database.resumeDao().create(resume(project))
        val b = database.resumeDao().create(resume(other))
        val repository = LocalResumesRepository(database.resumeDao())
        assertFailsWith<IllegalArgumentException> { repository.deleteMany(project, setOf(a, b)) }
        assertFailsWith<IllegalArgumentException> { repository.deleteMany(project, setOf(a, -1)) }
        assertFailsWith<IllegalArgumentException> { repository.deleteMany(project, emptySet()) }
        assertEquals(listOf(a), repository.observeResumes(project).first().map { it.id })
        assertEquals(listOf(b), repository.observeResumes(other).first().map { it.id })
    }

    @Test
    fun linkedVacancyProjectionTracksRenamesUnlinksAndDeletionWithinTheProject() =
        runBlocking<Unit> {
            val project = project()
            val other = project("Other")
            val resume = database.resumeDao().create(resume(project))
            val otherResume = database.resumeDao().create(resume(other))
            val vacancyDao = database.vacancyDao()
            val first = vacancyDao.create(
                vacancy(project).copy(name = "First", resumes = listOf(VacancyResume(resume))),
            )
            val second = vacancyDao.create(
                vacancy(project).copy(name = "Second", resumes = listOf(VacancyResume(resume))),
            )
            vacancyDao.create(vacancy(other).copy(resumes = listOf(VacancyResume(otherResume))))
            val repository = LocalResumesRepository(database.resumeDao())
            assertEquals(
                listOf(first, second),
                repository.observeLinkedVacancies(project).first().map {
                    it.vacancyId
                },
            )
            vacancyDao.update(vacancyDao.findById(first)!!.toDomainModel().copy(name = "Renamed"))
            assertEquals(
                listOf("Renamed", "Second"),
                repository.observeLinkedVacancies(project).first().map {
                    it.name
                },
            )
            vacancyDao.update(
                vacancyDao.findById(first)!!.toDomainModel().copy(resumes = emptyList()),
            )
            assertEquals(
                listOf(second),
                repository.observeLinkedVacancies(project).first().map {
                    it.vacancyId
                },
            )
            vacancyDao.delete(second, 500)
            assertEquals(emptyList(), repository.observeLinkedVacancies(project).first())
        }
}
