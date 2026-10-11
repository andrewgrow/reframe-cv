package reframecv.database.project

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import reframecv.shared.nowMillis

/** Stores the project hierarchy and permanently deletes project subtrees. */
@Dao
interface ProjectDao : ProjectQueries {
    @Transaction
    suspend fun insertWithParentMode(project: ProjectEntity): Long {
        project.parentId?.let { parentId ->
            val updatedRows = markAsContainer(parentId, project.createdAt)
            check(updatedRows == 1) {
                "Parent project is missing or configured as a workspace"
            }
        }
        return insert(project)
    }

    @Transaction
    suspend fun delete(project: ProjectEntity) {
        deleteSubtree(project.id)
    }

    @Query(
        "WITH RECURSIVE subtree(id) AS (SELECT id FROM projects WHERE id = :id UNION " +
            "SELECT projects.id FROM projects JOIN subtree ON projects.parent_id = subtree.id) " +
            "SELECT DISTINCT import_record_id FROM vacancies WHERE project_id IN " +
            "(SELECT id FROM subtree) AND import_record_id IS NOT NULL",
    )
    suspend fun subtreeImportIds(id: Long): List<Long>

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteEntity(id: Long): Int

    @Query(
        "DELETE FROM import_records WHERE id = :id AND NOT EXISTS " +
            "(SELECT 1 FROM vacancies WHERE import_record_id = :id)",
    )
    suspend fun deleteUnusedImportRecord(id: Long)

    @Transaction
    suspend fun deleteSubtree(id: Long): Int {
        val parentId = findById(id)?.parentId
        val sourceIds = subtreeImportIds(id)
        val changed = deleteEntity(id)
        sourceIds.forEach { deleteUnusedImportRecord(it) }
        if (changed > 0) parentId?.let { resetEmptyContainer(it, nowMillis()) }
        return changed
    }

    @Query(
        "UPDATE projects SET mode = 'Unconfigured', updated_at = :updatedAt " +
            "WHERE id = :id AND mode = 'Container' AND NOT EXISTS " +
            "(SELECT 1 FROM projects WHERE parent_id = :id)",
    )
    suspend fun resetEmptyContainer(id: Long, updatedAt: Long)
}
