package reframecv.repository

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import reframecv.attachments.AttachmentStorage
import reframecv.attachments.SnapshotChange
import reframecv.attachments.SnapshotInfo
import reframecv.attachments.StoredAttachment
import reframecv.attachments.storedAttachment
import reframecv.database.importrecord.ImportRecordDao
import reframecv.database.importrecord.ImportRecordEntity
import reframecv.database.vacancy.VacancyDao
import reframecv.domain.models.importrecord.ImportRecord
import reframecv.domain.models.vacancy.Vacancy
import reframecv.logging.ApplicationLogger

/** Serializes snapshot writes and cleanup so saved records keep their file copies. */
class VacancySnapshotManager(
    private val vacancies: VacancyDao,
    private val imports: ImportRecordDao,
    private val storage: AttachmentStorage,
    private val logger: ApplicationLogger,
) {
    private val mutex = Mutex()
    private var cleaned = false

    suspend fun cleanup() = mutex.withLock {
        if (!cleaned) {
            reportCleanupFailure("Clean orphaned snapshots") {
                storage.cleanup(imports.filePaths().toSet())
                cleaned = true
            }
        }
    }

    suspend fun snapshot(vacancyId: Long): SnapshotInfo? = mutex.withLock {
        val record = source(vacancyId) ?: return@withLock null
        SnapshotInfo(record, record.storedAttachment()?.let { storage.status(it) })
    }

    suspend fun open(vacancyId: Long) = mutex.withLock {
        val path = checkNotNull(source(vacancyId)?.filePath) { "Snapshot is missing" }
        storage.open(path)
    }

    suspend fun create(record: Vacancy, change: SnapshotChange): Long = mutex.withLock {
        withContext(NonCancellable) {
            withNewFile(record, change) { snapshot ->
                vacancies.createWithSnapshot(
                    if (change ==
                        SnapshotChange.Remove
                    ) {
                        record.copy(importRecordId = null)
                    } else {
                        record
                    },
                    snapshot,
                )
            }
        }
    }

    suspend fun update(record: Vacancy, change: SnapshotChange) = mutex.withLock {
        withContext(NonCancellable) {
            val previousPath = source(record.id)?.filePath
            withNewFile(record, change) { snapshot ->
                vacancies.updateWithSnapshot(record, snapshot, change == SnapshotChange.Remove)
            }
            previousPath?.let { deleteIfUnreferenced(it) }
        }
    }

    suspend fun delete(id: Long, timestamp: Long) = mutex.withLock {
        withContext(NonCancellable) {
            val previousPath = source(id)?.filePath
            check(vacancies.delete(id, timestamp) == 1) { "Active record does not exist" }
            previousPath?.let { deleteIfUnreferenced(it) }
        }
    }

    private suspend fun source(vacancyId: Long): ImportRecord? {
        val id = vacancies.findById(vacancyId)?.entity?.importRecordId ?: return null
        return imports.findById(id)?.toDomainModel()
    }

    private suspend fun <T> withNewFile(
        record: Vacancy,
        change: SnapshotChange,
        save: suspend (ImportRecordEntity?) -> T,
    ): T {
        val attachment = (change as? SnapshotChange.Attach)?.let { storage.store(it.selection) }
        var committed = false
        try {
            val result = save(attachment?.toImportRecord(record))
            committed = true
            return result
        } finally {
            if (!committed && attachment != null) {
                reportCleanupFailure("Remove failed snapshot copy") {
                    storage.delete(attachment.filePath)
                }
            }
        }
    }

    private fun StoredAttachment.toImportRecord(vacancy: Vacancy) =
        ImportRecordEntity.fromDomainModel(
            ImportRecord(
                provider = "file",
                createdAt = vacancy.updatedAt,
                sourceUrl = vacancy.url.takeIf { it.isNotBlank() },
                filePath = filePath,
                fileName = fileName,
                mediaType = mediaType,
                fileSize = fileSize,
                sha256 = sha256,
            ),
        )

    private suspend fun deleteIfUnreferenced(path: String) {
        reportCleanupFailure("Remove unused snapshot file") {
            if (!imports.referencesFile(path)) storage.delete(path)
        }
    }

    // Cleanup failures must preserve the outcome of the database transaction.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun reportCleanupFailure(operation: String, action: suspend () -> Unit) {
        try {
            action()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.error(operation, error, emptyMap())
        }
    }
}
