package reframecv.database.vacancy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest

class VacancyOrderingTest : WorkspaceDatabaseTest() {
    @Test
    fun listAndKeywordSearchKeepAscendingIdsAfterEditing() = runBlocking {
        val projectId = project()
        val dao = database.vacancyDao()
        val first = dao.create(vacancy(projectId).copy(name = "Zebra", updatedAt = 500))
        dao.create(vacancy(project("Other project")))
        val second = dao.create(vacancy(projectId).copy(name = "Alpha", updatedAt = 900))
        val third = dao.create(vacancy(projectId).copy(name = "Middle", updatedAt = 100))
        val expected = listOf(first, second, third)
        assertEquals(expected, dao.observeActive(projectId).first().map { it.entity.id })
        assertEquals(expected, dao.search(projectId, " KOTLIN ").map { it.entity.id })
        val edited = checkNotNull(dao.findById(first)).toDomainModel()
        dao.update(edited.copy(name = "Renamed", updatedAt = 1000))
        assertEquals(expected, dao.observeActive(projectId).first().map { it.entity.id })
        assertEquals(expected, dao.search(projectId, "kotlin").map { it.entity.id })
        dao.delete(second, 1100)
        assertEquals(
            listOf(first, third),
            dao.observeActive(projectId).first().map {
                it.entity.id
            },
        )
        assertEquals(listOf(first, third), dao.search(projectId, "kotlin").map { it.entity.id })
    }
}
