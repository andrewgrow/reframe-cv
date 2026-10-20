package reframecv.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.vacancy.VacancyResume

class ResumeAdaptationPersistenceTest : WorkspaceDatabaseTest() {
    @Test
    fun createsIndependentResumeAndAppendsLinkWithoutChangingOtherVacancyFields() = runBlocking {
        val project = project()
        val repository = LocalResumesRepository(database.resumeDao())
        val source = repository.createSaved(resume(project), MainResumeChange.Select)
        val vacancy = vacancy(project).copy(resumes = listOf(VacancyResume(source.id, 77)))
        val vacancyId = database.vacancyDao().create(vacancy)
        val result = repository.createAdaptation(
            source.copy(
                id = 0,
                name = "AI version",
                sourceResumeId = source.id,
                keywords = emptyList(),
            ),
            vacancyId,
        )
        val stored = database.vacancyDao().findById(vacancyId)!!.toDomainModel()
        assertEquals(listOf(VacancyResume(source.id, 77), VacancyResume(result.id)), stored.resumes)
        assertEquals(vacancy.name, stored.name)
        assertEquals(vacancy.description, stored.description)
        assertEquals(source.id, database.resumeDao().observeMainResumeId(project).first())
        assertEquals(source.id, result.sourceResumeId)
        assertEquals(emptyList(), result.keywords)
        assertNull(result.photoPath)
        assertEquals(2, repository.observeResumes(project).first().size)
    }

    @Test
    fun missingOrForeignVacancyAndMissingSourceNeverLeaveAnOrphanResume() = runBlocking {
        val project = project()
        val repository = LocalResumesRepository(database.resumeDao())
        val source = repository.createSaved(resume(project), MainResumeChange.Keep)
        val other = project("Other")
        val foreignVacancy = database.vacancyDao().create(vacancy(other))
        val draft = source.copy(id = 0, sourceResumeId = source.id, keywords = emptyList())
        listOf(999L, foreignVacancy).forEach { id ->
            assertFailsWith<IllegalArgumentException> { repository.createAdaptation(draft, id) }
            assertEquals(1, repository.observeResumes(project).first().size)
        }
        val vacancyId = database.vacancyDao().create(vacancy(project))
        assertFailsWith<IllegalArgumentException> {
            repository.createAdaptation(draft.copy(sourceResumeId = 999), vacancyId)
        }
        assertEquals(1, repository.observeResumes(project).first().size)
        assertEquals(
            emptyList(),
            database.vacancyDao().findById(vacancyId)!!.toDomainModel().resumes,
        )
    }

    @Test
    fun newVacancyCanKeepAnAdaptationInTheProjectBeforeTheVacancyIsSaved() = runBlocking {
        val project = project()
        val repository = LocalResumesRepository(database.resumeDao())
        val source = repository.createSaved(resume(project), MainResumeChange.Keep)
        val result = repository.createAdaptation(
            source.copy(id = 0, sourceResumeId = source.id, keywords = emptyList()),
            null,
        )
        assertEquals(source.id, result.sourceResumeId)
        assertEquals(2, repository.observeResumes(project).first().size)
        assertEquals(emptyList(), database.vacancyDao().observeActive(project).first())
    }
}
