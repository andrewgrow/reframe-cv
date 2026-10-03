package reframecv.database.project

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
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

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query(
        "WITH RECURSIVE subtree(id, depth) AS (" +
            "SELECT id, 0 FROM projects WHERE id = :id UNION ALL " +
            "SELECT projects.id, subtree.depth + 1 FROM projects " +
            "JOIN subtree ON projects.parent_id = subtree.id) " +
            "SELECT id FROM subtree ORDER BY depth DESC",
    )
    suspend fun findSubtreeIds(id: Long): List<Long>

    @Transaction
    suspend fun deleteSubtree(id: Long) {
        val ids = findSubtreeIds(id)
        check(ids.isNotEmpty()) { "Project does not exist" }
        ids.forEach { projectId ->
            check(deleteById(projectId) == 1) { "Project does not exist" }
        }
    }

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun findById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects ORDER BY updated_at DESC, name ASC")
    fun observeAll(): Flow<List<ProjectEntity>>
}
