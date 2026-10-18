package reframecv.database.prompts

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/** Persists prompt drafts and filters enabled instructions by their intended feature. */
@Dao
abstract class PromptsDao {
    @Query("SELECT * FROM prompts ORDER BY id ASC")
    abstract fun observeAll(): Flow<List<PromptEntity>>

    @Query(
        "SELECT * FROM prompts WHERE active = 1 AND (component IS NULL OR component = :component) ORDER BY id ASC",
    )
    abstract fun observeAvailable(component: String): Flow<List<PromptEntity>>

    @Query("SELECT * FROM prompts WHERE id = :id")
    abstract suspend fun get(id: Long): PromptEntity?

    @Insert
    abstract suspend fun insert(prompt: PromptEntity): Long

    @Update
    abstract suspend fun update(prompt: PromptEntity): Int

    @Transaction
    open suspend fun save(prompt: PromptEntity): Long {
        if (prompt.id == 0L) return insert(prompt)
        val previous = checkNotNull(get(prompt.id)) { "Prompt does not exist" }
        check(update(prompt.copy(createdAt = previous.createdAt)) == 1)
        return prompt.id
    }
}
