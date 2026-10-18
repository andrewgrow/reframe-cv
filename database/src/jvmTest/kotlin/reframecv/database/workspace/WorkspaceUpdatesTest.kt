package reframecv.database.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.vacancy.VacancyResume

class WorkspaceUpdatesTest : WorkspaceDatabaseTest() {
    @Test
    fun rejectsMovesAndInvalidLinksWithoutChangingRecordsOrKeywords() = runBlocking {
        val projectId = project()
        val otherId = project("Other")
        val resumeId = database.resumeDao().create(resume(projectId))
        val letterId = database.coverLetterDao().create(letter(projectId))
        val vacancyId = database.vacancyDao().create(vacancy(projectId))
        val storedResume = requireNotNull(database.resumeDao().findById(resumeId)).toDomainModel()
        val storedLetter = requireNotNull(
            database.coverLetterDao().findById(letterId),
        ).toDomainModel()
        val storedVacancy = requireNotNull(
            database.vacancyDao().findById(vacancyId),
        ).toDomainModel()
        assertFailsWith<IllegalArgumentException> {
            database.resumeDao().update(storedResume.copy(projectId = otherId))
        }
        assertFailsWith<IllegalArgumentException> {
            database.coverLetterDao().update(storedLetter.copy(projectId = otherId))
        }
        assertFailsWith<IllegalArgumentException> {
            database.vacancyDao().update(storedVacancy.copy(projectId = otherId))
        }
        assertFailsWith<IllegalArgumentException> {
            database.resumeDao().update(
                storedResume.copy(sourceResumeId = -1, keywords = listOf("Java")),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            database.vacancyDao().update(
                storedVacancy.copy(resumes = listOf(VacancyResume(-1)), keywords = listOf("Java")),
            )
        }
        assertEquals(storedResume, database.resumeDao().findById(resumeId)?.toDomainModel())
        assertEquals(storedLetter, database.coverLetterDao().findById(letterId)?.toDomainModel())
        assertEquals(storedVacancy, database.vacancyDao().findById(vacancyId)?.toDomainModel())
    }

    @Test
    fun missingAndDeletedRecordsCannotBeUpdated() = runBlocking {
        val projectId = project()
        assertFailsWith<IllegalStateException> {
            database.resumeDao().update(resume(projectId).copy(id = -1))
        }
        assertFailsWith<IllegalStateException> {
            database.coverLetterDao().update(letter(projectId).copy(id = -1))
        }
        assertFailsWith<IllegalStateException> {
            database.vacancyDao().update(vacancy(projectId).copy(id = -1))
        }
        val resumeId = database.resumeDao().create(resume(projectId))
        val letterId = database.coverLetterDao().create(letter(projectId))
        val vacancyId = database.vacancyDao().create(vacancy(projectId))
        database.resumeDao().delete(resumeId, 300)
        database.coverLetterDao().delete(letterId, 300)
        database.vacancyDao().delete(vacancyId, 300)
        assertFailsWith<IllegalStateException> {
            database.resumeDao().update(resume(projectId).copy(id = resumeId))
        }
        assertFailsWith<IllegalStateException> {
            database.coverLetterDao().update(letter(projectId).copy(id = letterId))
        }
        assertFailsWith<IllegalStateException> {
            database.vacancyDao().update(vacancy(projectId).copy(id = vacancyId))
        }
        assertEquals(0, database.resumeDao().delete(-1, 400))
        assertEquals(0, database.coverLetterDao().delete(-1, 400))
    }
}
