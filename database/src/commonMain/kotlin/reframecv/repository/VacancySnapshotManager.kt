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
import reframecv.database.project.ProjectDao
import reframecv.database.vacancy.VacancyDao
import reframecv.database.vacancyattachment.VacancyAttachmentDao
import reframecv.database.vacancyattachment.VacancyAttachmentEntity
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancyattachment.VacancyAttachment
import reframecv.logging.ApplicationLogger

/** Serializes snapshot writes and cleanup so saved records keep their file copies. */
class VacancySnapshotManager(
    private val vacancies: VacancyDao,
    private val attachments: VacancyAttachmentDao,
    private val storage: AttachmentStorage,
    private val logger: ApplicationLogger,
) {
    private val mutex = Mutex()
    private var cleaned = false

    suspend fun cleanup() = mutex.withLock {
        if (!cleaned) {
            logger.reportCleanupFailure("Clean orphaned snapshots") {
                storage.cleanup(attachments.filePaths().toSet())
                cleaned = true
            }
        }
    }

    suspend fun snapshot(vacancyId: Long): SnapshotInfo? = mutex.withLock {
        val record = source(vacancyId) ?: return@withLock null
        SnapshotInfo(record, storage.status(record.storedAttachment()))
    }

    suspend fun open(vacancyId: Long) = mutex.withLock {
        val path = checkNotNull(source(vacancyId)?.filePath) { "Snapshot is missing" }
        storage.open(path)
    }

    suspend fun create(record: Vacancy, change: SnapshotChange): Long =
        createSaved(record, change).id

    suspend fun createSaved(record: Vacancy, change: SnapshotChange): Vacancy = mutex.withLock {
        require(change !is SnapshotChange.Parsed) { "A saved attachment is required" }
        withContext(NonCancellable) {
            withNewFile(record, change) { snapshot ->
                vacancies.createSavedWithSnapshot(
                    if (change ==
                        SnapshotChange.Remove
                    ) {
                        record.copy(vacancyAttachmentId = null)
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
                vacancies.updateWithSnapshot(
                    record,
                    snapshot,
                    change == SnapshotChange.Remove,
                    change as? SnapshotChange.Parsed,
                )
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

    suspend fun deleteProject(projects: ProjectDao, id: Long): Int = mutex.withLock {
        withContext(NonCancellable) {
            val paths = projects.subtreeAttachmentIds(id).mapNotNull {
                attachments.findById(it)?.filePath
            }
            val changed = projects.deleteSubtree(id)
            paths.forEach { deleteIfUnreferenced(it) }
            changed
        }
    }

    private suspend fun source(vacancyId: Long): VacancyAttachment? {
        val id = vacancies.findById(vacancyId)?.entity?.vacancyAttachmentId ?: return null
        return attachments.findById(id)?.toDomainModel()
    }

    private suspend fun <T> withNewFile(
        record: Vacancy,
        change: SnapshotChange,
        save: suspend (VacancyAttachmentEntity?) -> T,
    ): T {
        val selection = change as? SnapshotChange.Attach
        val attachment = selection?.let { storage.store(it.selection) }
        var committed = false
        try {
            val result = save(
                attachment?.toVacancyAttachment(record)?.copy(
                    parsedAt = selection?.parsedAt,
                ),
            )
            committed = true
            return result
        } finally {
            if (!committed && attachment != null) {
                logger.reportCleanupFailure("Remove failed snapshot copy") {
                    storage.delete(attachment.filePath)
                }
            }
        }
    }

    private suspend fun deleteIfUnreferenced(path: String) {
        logger.reportCleanupFailure("Remove unused snapshot file") {
            if (!attachments.referencesFile(path)) storage.delete(path)
        }
    }
}

// Cleanup failures must preserve the outcome of the database transaction.
@Suppress("TooGenericExceptionCaught")
private suspend fun ApplicationLogger.reportCleanupFailure(
    operation: String,
    action: suspend () -> Unit,
) {
    try {
        action()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        this.error(operation, error, emptyMap())
    }
}

private fun StoredAttachment.toVacancyAttachment(vacancy: Vacancy) =
    VacancyAttachmentEntity.fromDomainModel(
        VacancyAttachment(
            createdAt = vacancy.updatedAt,
            sourceUrl = vacancy.url.takeIf { it.isNotBlank() },
            filePath = filePath,
            fileName = fileName,
            mediaType = mediaType,
            fileSize = fileSize,
            sha256 = sha256,
        ),
    )
