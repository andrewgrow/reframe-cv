package reframecv.database.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class VacancyLettersTest : WorkspaceDatabaseTest() {
    @Test
    fun persistsOrderedReusableLinksAndUnlinkKeepsTheLetter() = runBlocking<Unit> {
        val project = project()
        val a = database.coverLetterDao().create(letter(project))
        val b = database.coverLetterDao().create(letter(project).copy(name = "Second"))
        val dao = database.vacancyDao()
        val id = dao.create(vacancy(project).copy(coverLetterIds = listOf(b, a)))
        val second = dao.create(vacancy(project).copy(coverLetterIds = listOf(a)))
        assertEquals(listOf(b, a), dao.findById(id)!!.toDomainModel().coverLetterIds)
        dao.update(dao.findById(id)!!.toDomainModel().copy(coverLetterIds = listOf(b)))
        assertEquals(listOf(a), dao.findById(second)!!.toDomainModel().coverLetterIds)
        assertEquals(a, database.coverLetterDao().findById(a)!!.entity.id)
        database.coverLetterDao().delete(a, 500)
        assertEquals(emptyList(), dao.findById(second)!!.toDomainModel().coverLetterIds)
        dao.delete(id, 600)
        assertEquals(b, database.coverLetterDao().findById(b)!!.entity.id)
    }

    @Test
    fun createsGeneratedLetterAndLinkAtomicallyWithoutSavingVacancyDraftFields() =
        runBlocking<Unit> {
            val project = project()
            val resume = database.resumeDao().create(resume(project))
            val existing = database.coverLetterDao().create(letter(project))
            val vacancy = database.vacancyDao().create(
                vacancy(project).copy(name = "Stored", coverLetterIds = listOf(existing)),
            )
            val saved = database.coverLetterDao().createAndLink(
                letter(project).copy(name = "Generated", sourceResumeId = resume),
                vacancy,
            )
            assertEquals(
                resume,
                database.coverLetterDao().findById(saved.id)!!.entity.sourceResumeId,
            )
            val stored = database.vacancyDao().findById(vacancy)!!.toDomainModel()
            assertEquals("Stored", stored.name)
            assertEquals(listOf(existing, saved.id), stored.coverLetterIds)
            database.resumeDao().delete(resume, 500)
            assertNull(database.coverLetterDao().findById(saved.id)!!.entity.sourceResumeId)
            assertEquals("Dear team", database.coverLetterDao().findById(saved.id)!!.entity.content)
        }

    @Test
    fun rejectsMissingForeignAndDuplicateLinksAndRollsBackGeneratedDocuments() = runBlocking<Unit> {
        val project = project()
        val other = project("Other")
        val foreign = database.coverLetterDao().create(letter(other))
        val vacancy = database.vacancyDao().create(vacancy(other))
        assertFailsWith<IllegalArgumentException> {
            database.vacancyDao().create(vacancy(project).copy(coverLetterIds = listOf(foreign)))
        }
        assertFailsWith<IllegalArgumentException> {
            database.coverLetterDao().createAndLink(letter(project), vacancy)
        }
        assertFailsWith<IllegalArgumentException> {
            database.coverLetterDao().createAndLink(letter(project), -1)
        }
        val source = database.resumeDao().create(resume(other))
        assertFailsWith<IllegalArgumentException> {
            database.coverLetterDao().createAndLink(
                letter(project).copy(sourceResumeId = source),
                null,
            )
        }
        assertEquals(emptyList(), database.coverLetterDao().observeActive(project).first())
        val own = database.coverLetterDao().create(letter(project))
        assertFailsWith<IllegalArgumentException> {
            database.vacancyDao().create(vacancy(project).copy(coverLetterIds = listOf(own, own)))
        }
    }
}
