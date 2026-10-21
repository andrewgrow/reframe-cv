package reframecv.database.vacancy

import androidx.room3.Transaction
import reframecv.database.workspace.WorkspaceDao
import reframecv.domain.models.screenstate.ScreenType

/** Deletes single or selected vacancies atomically, including unused snapshots and screen state. */
interface VacancyDeletionQueries :
    VacancyQueries,
    WorkspaceDao {
    @Transaction
    suspend fun deleteMany(projectId: Long, ids: Set<Long>, updatedAt: Long) {
        ids.forEach { id ->
            require(id > 0 && findById(id)?.entity?.projectId == projectId) {
                "Selected vacancies must exist in this project"
            }
        }
        ids.forEach { check(delete(it, updatedAt) == 1) }
    }

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
