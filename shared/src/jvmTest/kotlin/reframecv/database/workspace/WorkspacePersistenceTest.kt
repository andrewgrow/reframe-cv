package reframecv.database.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WorkspacePersistenceTest : WorkspaceDatabaseTest() {
    @Test
    fun resumesRoundTripWithIndependentAdaptationAndAtomicKeywordUpdates() = runBlocking {
        val projectId = project()
        val dao = database.resumeDao()
        val source = resume(projectId)
        val sourceId = dao.create(source)
        val adaptation = source.copy(
            name = "Adapted",
            sourceResumeId = sourceId,
            content = "Adapted text",
        )
        val id = dao.create(adaptation)
        val stored = adaptation.copy(id = id, keywords = listOf("Kotlin", "Android", "Résumé"))
        assertEquals(stored, dao.findById(id)?.toDomainModel())
        val updated = stored.copy(
            content = "New text",
            updatedAt = 300,
            keywords = listOf(" Java ", "JAVA"),
        )
        dao.update(updated)
        assertEquals(updated.copy(keywords = listOf("Java")), dao.findById(id)?.toDomainModel())
        assertEquals(source.content, dao.findById(sourceId)?.entity?.content)
        assertEquals(listOf(sourceId), dao.search(projectId, " KOTLIN ").map { it.entity.id })
        assertEquals(listOf(id), dao.search(projectId, "java").map { it.entity.id })
        assertEquals(
            listOf(id, sourceId),
            dao.observeActive(projectId).first().map {
                it.entity.id
            },
        )
        assertEquals(2, dao.observeCount(projectId).first())
        dao.update(updated.copy(keywords = emptyList()))
        assertEquals(emptyList(), dao.findById(id)?.toDomainModel()?.keywords)
        assertEquals(emptyList(), dao.search(projectId, "Java"))
        assertNull(dao.findById(-1))
    }

    @Test
    fun lettersAndVacanciesRoundTripAndSearchTheirOwnKeywords() = runBlocking {
        val projectId = project()
        val letter = letter(projectId)
        val vacancy = vacancy(projectId)
        val letterDao = database.coverLetterDao()
        val vacancyDao = database.vacancyDao()
        val letterId = letterDao.create(letter)
        val vacancyId = vacancyDao.create(vacancy)
        val storedLetter = letter.copy(
            id = letterId,
            keywords = listOf("Kotlin", "Android", "Résumé"),
        )
        val storedVacancy = vacancy.copy(id = vacancyId, keywords = storedLetter.keywords)
        assertEquals(storedLetter, letterDao.findById(letterId)?.toDomainModel())
        assertEquals(storedVacancy, vacancyDao.findById(vacancyId)?.toDomainModel())
        letterDao.update(
            storedLetter.copy(content = "Changed", updatedAt = 300, keywords = listOf("Java")),
        )
        vacancyDao.update(
            storedVacancy.copy(description = "Changed", updatedAt = 300, keywords = listOf("Java")),
        )
        assertEquals("Changed", letterDao.findById(letterId)?.entity?.content)
        assertEquals("Changed", vacancyDao.findById(vacancyId)?.entity?.description)
        assertEquals(listOf("Java"), letterDao.findById(letterId)?.toDomainModel()?.keywords)
        assertEquals(listOf("Java"), vacancyDao.findById(vacancyId)?.toDomainModel()?.keywords)
        assertEquals(emptyList(), letterDao.search(projectId, "kotlin"))
        assertEquals(emptyList(), vacancyDao.search(projectId, "kotlin"))
        assertEquals(listOf(letterId), letterDao.search(projectId, " JAVA ").map { it.entity.id })
        assertEquals(listOf(vacancyId), vacancyDao.search(projectId, " JAVA ").map { it.entity.id })
        assertNull(letterDao.findById(-1))
        assertNull(vacancyDao.findById(-1))
        assertEquals(1, letterDao.observeCount(projectId).first())
        assertEquals(1, vacancyDao.observeCount(projectId).first())
    }

    @Test
    fun allRecordsAndKeywordsPersistAfterReopening() = runBlocking {
        val projectId = project()
        val resumeId = database.resumeDao().create(resume(projectId))
        val letterId = database.coverLetterDao().create(letter(projectId))
        val vacancyId = database.vacancyDao().create(
            vacancy(projectId).copy(resumeId = resumeId, coverLetterId = letterId),
        )
        assertEquals(1, database.projectDao().setMainResume(projectId, resumeId, 300))
        val beforeResume = database.resumeDao().findById(resumeId)?.toDomainModel()
        val beforeLetter = database.coverLetterDao().findById(letterId)?.toDomainModel()
        val beforeVacancy = database.vacancyDao().findById(vacancyId)?.toDomainModel()
        database.close()
        openDatabase()
        assertEquals(beforeResume, database.resumeDao().findById(resumeId)?.toDomainModel())
        assertEquals(beforeLetter, database.coverLetterDao().findById(letterId)?.toDomainModel())
        assertEquals(beforeVacancy, database.vacancyDao().findById(vacancyId)?.toDomainModel())
        assertEquals(resumeId, database.projectDao().findById(projectId)?.mainResumeId)
        assertEquals(
            listOf(resumeId),
            database.resumeDao().search(projectId, "RÉSUMÉ").map {
                it.entity.id
            },
        )
    }
}
