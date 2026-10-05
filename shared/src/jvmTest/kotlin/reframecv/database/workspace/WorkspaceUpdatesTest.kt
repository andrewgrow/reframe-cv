package reframecv.database.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

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
                storedVacancy.copy(resumeId = -1, keywords = listOf("Java")),
            )
        }
        assertEquals(storedResume, database.resumeDao().findById(resumeId)?.toDomainModel())
        assertEquals(storedLetter, database.coverLetterDao().findById(letterId)?.toDomainModel())
        assertEquals(storedVacancy, database.vacancyDao().findById(vacancyId)?.toDomainModel())
    }

    @Test
    fun missingAndDeletedRecordsCannotBeUpdatedOrCreatedAsDeleted() = runBlocking {
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
        assertFailsWith<IllegalStateException> {
            database.resumeDao().create(resume(projectId).copy(deletedAt = 300))
        }
        assertFailsWith<IllegalStateException> {
            database.coverLetterDao().create(letter(projectId).copy(deletedAt = 300))
        }
        assertFailsWith<IllegalStateException> {
            database.vacancyDao().create(vacancy(projectId).copy(deletedAt = 300))
        }
        val resumeId = database.resumeDao().create(resume(projectId))
        val letterId = database.coverLetterDao().create(letter(projectId))
        val vacancyId = database.vacancyDao().create(vacancy(projectId))
        database.resumeDao().softDelete(resumeId, 300)
        database.coverLetterDao().softDelete(letterId, 300)
        database.vacancyDao().softDelete(vacancyId, 300)
        assertFailsWith<IllegalStateException> {
            database.resumeDao().update(resume(projectId).copy(id = resumeId))
        }
        assertFailsWith<IllegalStateException> {
            database.coverLetterDao().update(letter(projectId).copy(id = letterId))
        }
        assertFailsWith<IllegalStateException> {
            database.vacancyDao().update(vacancy(projectId).copy(id = vacancyId))
        }
        assertEquals(0, database.resumeDao().softDelete(-1, 400))
        assertEquals(0, database.coverLetterDao().softDelete(-1, 400))
    }
}
