package reframecv.database.importrecord

import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.importrecord.ImportRecord

class ImportRecordPersistenceTest : WorkspaceDatabaseTest() {
    private fun record() = ImportRecord(
        provider = "apify/curious_coder/linkedin-jobs-scraper",
        externalId = "3692563200",
        sourceUrl = "https://example.com/jobs/3692563200",
        rawData = """{"title":"Kotlin Developer","unknown":{"salary":null}}""",
        metadataJson = """{"runId":"run-1"}""",
        createdAt = 100,
    )

    @Test
    fun preservesRawSnapshotsAndAllowsRepeatedExternalIdentifiers() = runBlocking {
        val dao = database.importRecordDao()
        val original = record()
        val id = dao.insert(ImportRecordEntity.fromDomainModel(original))
        val newer = original.copy(rawData = """{"title":"Senior Developer"}""", createdAt = 200)
        val newerId = dao.insert(ImportRecordEntity.fromDomainModel(newer))
        assertNotEquals(id, newerId)
        assertEquals(original.copy(id = id), dao.findById(id)?.toDomainModel())
        assertEquals(newer.copy(id = newerId), dao.findById(newerId)?.toDomainModel())
        assertEquals(listOf(newerId, id), dao.observeAll().first().map { it.id })
        val stored = requireNotNull(dao.findById(id))
        assertEquals(stored, ImportRecordEntity.fromDomainModel(stored.toDomainModel()))
        assertNull(dao.findById(-1))
    }

    @Test
    fun storesOptionalMetadataAndManualVacanciesWithoutSources() = runBlocking {
        val minimal = ImportRecord(provider = "other", rawData = "{}", createdAt = 300)
        val id = database.importRecordDao().insert(ImportRecordEntity.fromDomainModel(minimal))
        assertEquals(
            minimal.copy(id = id),
            database.importRecordDao().findById(id)?.toDomainModel(),
        )
        val vacancyId = database.vacancyDao().create(vacancy(project()))
        assertNull(database.vacancyDao().findById(vacancyId)?.toDomainModel()?.importRecordId)
    }

    @Test
    fun vacancyEditingAndDeletionPreserveSharedSourceSnapshot() = runBlocking {
        val source = record()
        val sourceId = database.importRecordDao().insert(ImportRecordEntity.fromDomainModel(source))
        val dao = database.vacancyDao()
        val initial = vacancy(project()).copy(
            importRecordId = sourceId,
            keywords = listOf("Kotlin", "Android", "Résumé"),
        )
        val first = dao.create(initial)
        val second = dao.create(vacancy(project("Other project")).copy(importRecordId = sourceId))
        val edited = initial.copy(
            id = first,
            name = "Edited",
            description = "My text",
            updatedAt = 400,
        )
        dao.update(edited)
        assertEquals(edited, dao.findById(first)?.toDomainModel())
        assertEquals(1, dao.delete(first, 500))
        assertNull(dao.findById(first))
        assertEquals(sourceId, dao.findById(second)?.entity?.importRecordId)
        assertEquals(
            source.copy(id = sourceId),
            database.importRecordDao().findById(sourceId)?.toDomainModel(),
        )
    }

    @Test
    fun physicallyDeletesUnusedSnapshotButPreservesReferencedSnapshot() = runBlocking {
        val source = record()
        val dao = database.importRecordDao()
        val unusedId = dao.insert(ImportRecordEntity.fromDomainModel(source))
        val referencedId = dao.insert(ImportRecordEntity.fromDomainModel(source))
        val vacancyId = database.vacancyDao().create(
            vacancy(project()).copy(importRecordId = referencedId),
        )
        assertEquals(0, dao.deleteUnused(referencedId))
        assertEquals(source.copy(id = referencedId), dao.findById(referencedId)?.toDomainModel())
        assertEquals(
            referencedId,
            database.vacancyDao().findById(vacancyId)?.entity?.importRecordId,
        )
        assertEquals(1, dao.deleteUnused(unusedId))
        assertNull(dao.findById(unusedId))
        assertEquals(0, dao.deleteUnused(unusedId))
        assertEquals(0, dao.deleteUnused(-1))
        assertEquals(listOf(referencedId), dao.observeAll().first().map { it.id })
    }

    @Test
    fun rejectsMissingSourceAndRollsBackWorkspaceTransition() = runBlocking {
        val projectId = project()
        val before = database.projectDao().findById(projectId)
        val dao = database.vacancyDao()
        assertFailsWith<SQLiteException> {
            dao.create(vacancy(projectId).copy(importRecordId = -1))
        }
        assertEquals(before, database.projectDao().findById(projectId))
        assertEquals(emptyList(), dao.observeActive(projectId).first())
        val id = dao.create(vacancy(projectId))
        val stored = requireNotNull(dao.findById(id)).toDomainModel()
        assertFailsWith<SQLiteException> { dao.update(stored.copy(importRecordId = -1)) }
        assertEquals(stored, dao.findById(id)?.toDomainModel())
    }
}
