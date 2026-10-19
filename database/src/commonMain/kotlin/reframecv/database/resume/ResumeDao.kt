package reframecv.database.resume

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import reframecv.database.workspace.WorkspaceDao
import reframecv.database.workspace.normalizeKeywords
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.screenstate.ScreenType
import reframecv.repository.MainResumeChange

/** Saves, loads, and searches resumes in the local database. */
@Dao
interface ResumeDao :
    ResumeQueries,
    WorkspaceDao {
    @Query("SELECT main_resume_id FROM projects WHERE id = :projectId")
    fun observeMainResumeId(projectId: Long): Flow<Long?>

    @Query(
        "UPDATE projects SET main_resume_id = :resumeId, updated_at = :updatedAt " +
            "WHERE id = :projectId AND mode = 'Workspace' AND EXISTS " +
            "(SELECT 1 FROM resumes WHERE id = :resumeId AND project_id = :projectId)",
    )
    suspend fun selectMainResume(projectId: Long, resumeId: Long, updatedAt: Long): Int

    @Transaction
    suspend fun create(record: Resume, mainResumeChange: MainResumeChange): Long {
        val id = create(record)
        applyMainResumeChange(record.projectId, id, record.updatedAt, mainResumeChange)
        return id
    }

    @Transaction
    suspend fun update(record: Resume, mainResumeChange: MainResumeChange) {
        update(record)
        applyMainResumeChange(record.projectId, record.id, record.updatedAt, mainResumeChange)
    }

    private suspend fun applyMainResumeChange(
        projectId: Long,
        resumeId: Long,
        updatedAt: Long,
        change: MainResumeChange,
    ) {
        when (change) {
            MainResumeChange.Keep -> Unit

            MainResumeChange.Select -> check(
                selectMainResume(projectId, resumeId, updatedAt) == 1,
            ) {
                "Main resume must belong to the project workspace"
            }

            MainResumeChange.Clear -> clearMainResume(resumeId, updatedAt)
        }
    }

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
        updateLinkedVacancyTimestamps(id, updatedAt)
        val changed = deleteEntity(id)
        if (changed > 0) clearScreenStates(ScreenType.Resume.name, id)
        if (changed > 0) resetEmptyWorkspace(projectId, updatedAt)
        return changed
    }
}
