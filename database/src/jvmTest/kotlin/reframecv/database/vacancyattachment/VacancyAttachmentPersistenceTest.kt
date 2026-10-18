package reframecv.database.vacancyattachment

import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.vacancyattachment.VacancyAttachment

class VacancyAttachmentPersistenceTest : WorkspaceDatabaseTest() {
    private fun record() = VacancyAttachment(
        filePath = "attachments/source.pdf",
        fileName = "Résumé.pdf",
        mediaType = "application/pdf",
        fileSize = 42,
        sha256 = "original-hash",
        sourceUrl = "https://example.com/jobs/3692563200",
        createdAt = 100,
    )

    @Test
    fun preservesFileMetadataAndAllowsSharedFilePaths() = runBlocking {
        val dao = database.vacancyAttachmentDao()
        val original = record()
        val id = dao.insert(VacancyAttachmentEntity.fromDomainModel(original))
        val newer = original.copy(createdAt = 200, parsedAt = 250)
        val newerId = dao.insert(VacancyAttachmentEntity.fromDomainModel(newer))
        assertNotEquals(id, newerId)
        assertEquals(original.copy(id = id), dao.findById(id)?.toDomainModel())
        assertEquals(newer.copy(id = newerId), dao.findById(newerId)?.toDomainModel())
        assertEquals(listOf(newerId, id), dao.observeAll().first().map { it.id })
        val stored = requireNotNull(dao.findById(id))
        assertEquals(stored, VacancyAttachmentEntity.fromDomainModel(stored.toDomainModel()))
        assertNull(dao.findById(-1))
        database.close()
        openDatabase()
        assertEquals(
            original.copy(id = id),
            database.vacancyAttachmentDao().findById(id)?.toDomainModel(),
        )
        assertEquals(
            newer.copy(id = newerId),
            database.vacancyAttachmentDao().findById(newerId)?.toDomainModel(),
        )
    }

    @Test
    fun storesAttachmentsWithoutSourceUrlAndManualVacanciesWithoutAttachments() = runBlocking {
        val minimal = record().copy(sourceUrl = null, createdAt = 300)
        val id = database.vacancyAttachmentDao().insert(
            VacancyAttachmentEntity.fromDomainModel(minimal),
        )
        assertEquals(
            minimal.copy(id = id),
            database.vacancyAttachmentDao().findById(id)?.toDomainModel(),
        )
        val vacancyId = database.vacancyDao().create(vacancy(project()))
        assertNull(database.vacancyDao().findById(vacancyId)?.toDomainModel()?.vacancyAttachmentId)
    }

    @Test
    fun vacancyEditingAndDeletionPreserveSharedSourceSnapshot() = runBlocking {
        val source = record()
        val sourceId = database.vacancyAttachmentDao().insert(
            VacancyAttachmentEntity.fromDomainModel(source),
        )
        val dao = database.vacancyDao()
        val initial = vacancy(project()).copy(
            vacancyAttachmentId = sourceId,
            keywords = listOf("Kotlin", "Android", "Résumé"),
        )
        val first = dao.create(initial)
        val second = dao.create(
            vacancy(project("Other project")).copy(vacancyAttachmentId = sourceId),
        )
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
        assertEquals(sourceId, dao.findById(second)?.entity?.vacancyAttachmentId)
        assertEquals(
            source.copy(id = sourceId),
            database.vacancyAttachmentDao().findById(sourceId)?.toDomainModel(),
        )
    }

    @Test
    fun physicallyDeletesUnusedSnapshotButPreservesReferencedSnapshot() = runBlocking {
        val source = record()
        val dao = database.vacancyAttachmentDao()
        val unusedId = dao.insert(VacancyAttachmentEntity.fromDomainModel(source))
        val referencedId = dao.insert(VacancyAttachmentEntity.fromDomainModel(source))
        val vacancyId = database.vacancyDao().create(
            vacancy(project()).copy(vacancyAttachmentId = referencedId),
        )
        assertEquals(0, dao.deleteUnused(referencedId))
        assertEquals(source.copy(id = referencedId), dao.findById(referencedId)?.toDomainModel())
        assertEquals(
            referencedId,
            database.vacancyDao().findById(vacancyId)?.entity?.vacancyAttachmentId,
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
            dao.create(vacancy(projectId).copy(vacancyAttachmentId = -1))
        }
        assertEquals(before, database.projectDao().findById(projectId))
        assertEquals(emptyList(), dao.observeActive(projectId).first())
        val id = dao.create(vacancy(projectId))
        val stored = requireNotNull(dao.findById(id)).toDomainModel()
        assertFailsWith<SQLiteException> { dao.update(stored.copy(vacancyAttachmentId = -1)) }
        assertEquals(stored, dao.findById(id)?.toDomainModel())
    }
}
