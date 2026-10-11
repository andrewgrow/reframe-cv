package reframecv.database.resume

import androidx.room3.Dao
import androidx.room3.Transaction
import reframecv.database.workspace.WorkspaceDao
import reframecv.database.workspace.normalizeKeywords
import reframecv.domain.models.resume.Resume

/** Saves, loads, and searches resumes in the local database. */
@Dao
interface ResumeDao :
    ResumeQueries,
    WorkspaceDao {
    @Transaction
    suspend fun create(record: Resume): Long {
        requireWorkspace(record.projectId, record.createdAt)
        record.sourceResumeId?.let { requireActiveResume(it, record.projectId) }
        val id = insertEntity(ResumeEntity.fromDomainModel(record))
        replaceKeywords(id, record.keywords)
        return id
    }

    @Transaction
    suspend fun update(record: Resume) {
        val existing = checkNotNull(findById(record.id)) { "Record does not exist" }
        require(existing.entity.projectId == record.projectId) {
            "Cannot move a record to another project"
        }
        requireWorkspace(record.projectId, record.updatedAt)
        record.sourceResumeId?.let { requireActiveResume(it, record.projectId) }
        check(updateEntity(ResumeEntity.fromDomainModel(record)) == 1)
        replaceKeywords(record.id, record.keywords)
    }

    @Transaction
    suspend fun replaceKeywords(id: Long, keywords: List<String>) {
        clearKeywords(id)
        insertKeywords(
            normalizeKeywords(keywords).mapIndexed { position, keyword ->
                ResumeKeywordEntity(id, keyword, keyword.lowercase(), position)
            },
        )
    }

    @Transaction
    suspend fun search(projectId: Long, keyword: String): List<ResumeWithKeywords> =
        searchNormalized(projectId, keyword.trim().lowercase())

    @Transaction
    suspend fun delete(id: Long, updatedAt: Long): Int {
        val projectId = findById(id)?.entity?.projectId ?: return 0
        clearMainResume(id, updatedAt)
        clearSourceResume(id, updatedAt)
        clearVacancyResumes(id, updatedAt)
        val changed = deleteEntity(id)
        if (changed > 0) resetEmptyWorkspace(projectId, updatedAt)
        return changed
    }
}
