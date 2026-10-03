package reframecv.database.project

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("UPDATE projects SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateName(id: Long, name: String, updatedAt: Long): Int

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query(
        "WITH RECURSIVE subtree(id) AS (" +
            "SELECT id FROM projects WHERE id = :id UNION " +
            "SELECT projects.id FROM projects " +
            "JOIN subtree ON projects.parent_id = subtree.id) " +
            "UPDATE projects SET deleted_at = :deletedAt " +
            "WHERE id IN (SELECT id FROM subtree) AND deleted_at IS NULL",
    )
    suspend fun markSubtreeDeleted(id: Long, deletedAt: Long): Int

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun findById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE deleted_at IS NULL ORDER BY updated_at DESC, name ASC")
    fun observeAll(): Flow<List<ProjectEntity>>
}
