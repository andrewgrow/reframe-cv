package reframecv.database.project

import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** Provides reads and writes for project names, hierarchy, and workspace modes. */
interface ProjectQueries {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ProjectEntity): Long

    @Query(
        "UPDATE projects SET mode = 'Container', updated_at = :updatedAt " +
            "WHERE id = :id AND mode IN ('Unconfigured', 'Container')",
    )
    suspend fun markAsContainer(id: Long, updatedAt: Long): Int

    @Query(
        "UPDATE projects SET main_resume_id = :resumeId, updated_at = :updatedAt " +
            "WHERE id = :id AND mode = 'Workspace' AND " +
            "(:resumeId IS NULL OR EXISTS (SELECT 1 FROM resumes WHERE resumes.id = :resumeId " +
            "AND resumes.project_id = :id))",
    )
    suspend fun setMainResume(id: Long, resumeId: Long?, updatedAt: Long): Int

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("UPDATE projects SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateName(id: Long, name: String, updatedAt: Long): Int

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun findById(id: Long): ProjectEntity?

    @Query(
        "SELECT * FROM projects WHERE parent_id IS :parentId " +
            "ORDER BY id ASC",
    )
    fun observeChildren(parentId: Long?): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY id ASC")
    fun observeAll(): Flow<List<ProjectEntity>>
}
