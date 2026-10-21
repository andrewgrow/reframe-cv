package reframecv.database.resume

import androidx.room3.Transaction
import reframecv.database.workspace.WorkspaceDao
import reframecv.domain.models.screenstate.ScreenType

/** Deletes selected resumes atomically while clearing their references and view states. */
interface ResumeDeletionQueries :
    ResumeQueries,
    WorkspaceDao {
    @Transaction
    suspend fun deleteMany(projectId: Long, ids: Set<Long>, updatedAt: Long) {
        require(ids.isNotEmpty())
        ids.forEach { id ->
            require(findById(id)?.entity?.projectId == projectId) {
                "Resume is missing or belongs to another project"
            }
        }
        ids.forEach { check(delete(it, updatedAt) == 1) }
    }

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
