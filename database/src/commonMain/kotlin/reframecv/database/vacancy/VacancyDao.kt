package reframecv.database.vacancy

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import reframecv.assessment.ResumeAssessment
import reframecv.assessment.ResumeAssessmentJson
import reframecv.attachments.SnapshotChange
import reframecv.database.vacancyattachment.VacancyAttachmentEntity
import reframecv.database.workspace.WorkspaceDao
import reframecv.database.workspace.normalizeKeywords
import reframecv.domain.models.screenstate.ScreenType
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume

/** Saves, loads, searches, and permanently deletes vacancies in the local database. */
@Dao
interface VacancyDao :
    VacancyQueries,
    VacancyLetterQueries,
    WorkspaceDao {
    @Query("UPDATE vacancy_attachments SET parsed_at = :parsedAt WHERE id = :id")
    suspend fun markSnapshotParsed(id: Long, parsedAt: Long): Int

    @Transaction
    suspend fun create(record: Vacancy): Long {
        requireWorkspace(record.projectId, record.createdAt)
        val id = insertEntity(VacancyEntity.fromDomainModel(record))
        replaceLetters(id, record)
        replaceResumes(id, record)
        replaceKeywords(id, record.keywords)
        return id
    }

    @Transaction
    suspend fun update(record: Vacancy) {
        val existing = checkNotNull(findById(record.id)) { "Record does not exist" }
        require(existing.entity.projectId == record.projectId) {
            "Cannot move a record to another project"
        }
        requireWorkspace(record.projectId, record.updatedAt)
        check(updateEntity(VacancyEntity.fromDomainModel(record)) == 1)
        replaceLetters(record.id, record)
        replaceResumes(record.id, record)
        replaceKeywords(record.id, record.keywords)
    }

    @Transaction
    suspend fun createWithSnapshot(record: Vacancy, snapshot: VacancyAttachmentEntity?): Long =
        createSavedWithSnapshot(record, snapshot).id

    @Transaction
    suspend fun createSavedWithSnapshot(
        record: Vacancy,
        snapshot: VacancyAttachmentEntity?,
    ): Vacancy {
        val sourceId = snapshot?.let { insertSnapshot(it) } ?: record.vacancyAttachmentId
        val saved = record.copy(vacancyAttachmentId = sourceId)
        return saved.copy(id = create(saved))
    }

    @Transaction
    suspend fun updateWithSnapshot(
        record: Vacancy,
        snapshot: VacancyAttachmentEntity?,
        removeSnapshot: Boolean,
        parsed: SnapshotChange.Parsed? = null,
    ) {
        val previous = checkNotNull(findById(record.id)).entity.vacancyAttachmentId
        val sourceId = when {
            snapshot != null -> insertSnapshot(snapshot)
            removeSnapshot -> null
            else -> previous
        }
        if (parsed != null) {
            check(previous == parsed.attachmentId && sourceId == previous) {
                "Parsed source no longer matches the vacancy attachment"
            }
            check(markSnapshotParsed(parsed.attachmentId, parsed.parsedAt) == 1)
        }
        update(record.copy(vacancyAttachmentId = sourceId))
        if (previous != null && previous != sourceId) deleteUnusedVacancyAttachment(previous)
    }

    @Transaction
    suspend fun saveResumeAssessment(
        vacancyId: Long,
        projectId: Long,
        resumeId: Long,
        assessment: ResumeAssessment,
        updatedAt: Long,
    ) {
        val stored = checkNotNull(findById(vacancyId)) { "Vacancy no longer exists" }
        require(stored.entity.projectId == projectId)
        requireActiveResume(resumeId, projectId)
        val record = stored.toDomainModel()
        val link = VacancyResume(resumeId, assessment.result.score, assessment)
        val links = if (record.resumes.any { it.resumeId == resumeId }) {
            record.resumes.map { if (it.resumeId == resumeId) link else it }
        } else {
            record.resumes + link
        }
        replaceResumes(vacancyId, record.copy(resumes = links))
        check(updateEntity(stored.entity.copy(updatedAt = updatedAt)) == 1)
    }

    @Transaction
    suspend fun replaceResumes(id: Long, record: Vacancy) {
        require(record.resumes.map { it.resumeId }.distinct().size == record.resumes.size) {
            "A resume can only be linked once"
        }
        record.resumes.forEach { requireActiveResume(it.resumeId, record.projectId) }
        clearResumes(id)
        insertResumes(
            record.resumes.mapIndexed { position, link ->
                VacancyResumeEntity(
                    id,
                    link.resumeId,
                    link.atsScore,
                    position,
                    link.assessment?.let(ResumeAssessmentJson::encode),
                )
            },
        )
    }

    @Transaction
    suspend fun replaceKeywords(id: Long, keywords: List<String>) {
        clearKeywords(id)
        insertKeywords(
            normalizeKeywords(keywords).mapIndexed { position, keyword ->
                VacancyKeywordEntity(id, keyword, keyword.lowercase(), position)
            },
        )
    }

    @Transaction
    suspend fun search(projectId: Long, keyword: String): List<VacancyWithKeywords> =
        searchNormalized(projectId, keyword.trim().lowercase())

    /** Delete one vacancy and its unused source snapshot atomically. */
    @Transaction
    suspend fun delete(id: Long, updatedAt: Long): Int {
        val existing = findById(id)?.entity ?: return 0
        val changed = deleteEntity(id)
        if (changed > 0) clearScreenStates(ScreenType.Vacancy.name, id)
        if (changed > 0) {
            existing.vacancyAttachmentId?.let { deleteUnusedVacancyAttachment(it) }
            resetEmptyWorkspace(existing.projectId, updatedAt)
        }
        return changed
    }
}
