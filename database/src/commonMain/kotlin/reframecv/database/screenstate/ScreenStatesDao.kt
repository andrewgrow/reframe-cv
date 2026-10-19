package reframecv.database.screenstate

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.serialization.json.JsonObject
import reframecv.domain.models.screenstate.ScreenState
import reframecv.domain.models.screenstate.ScreenStateOwner

/** Atomically patches a screen's JSON fields while preserving unrelated state. */
@Dao
interface ScreenStatesDao {
    @Query(
        "SELECT * FROM screen_states WHERE screen = :screen AND document_id IS :documentId " +
            "AND project_id IS :projectId",
    )
    suspend fun get(screen: String, documentId: Long?, projectId: Long?): ScreenStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: ScreenStateEntity)

    @Query(
        "SELECT CASE :screen " +
            "WHEN 'Vacancy' THEN EXISTS(SELECT 1 FROM vacancies " +
            "WHERE id = :id AND project_id = :projectId) " +
            "WHEN 'Resume' THEN EXISTS(SELECT 1 FROM resumes " +
            "WHERE id = :id AND project_id = :projectId) " +
            "WHEN 'CoverLetter' THEN EXISTS(SELECT 1 FROM cover_letters " +
            "WHERE id = :id AND project_id = :projectId) " +
            "WHEN 'Project' THEN EXISTS(SELECT 1 FROM projects " +
            "WHERE id = :id AND id = :projectId) " +
            "WHEN 'Settings' THEN :id IS NULL AND :projectId IS NULL " +
            "WHEN 'Application' THEN :id IS NULL AND :projectId IS NULL ELSE 0 END",
    )
    suspend fun ownerExists(screen: String, id: Long?, projectId: Long?): Boolean

    @Transaction
    suspend fun update(owner: ScreenStateOwner, changes: JsonObject) {
        if (!ownerExists(owner.screen.name, owner.documentId, owner.projectId)) return
        val previous =
            get(owner.screen.name, owner.documentId, owner.projectId)?.toDomainModel()?.states
                ?: JsonObject(emptyMap())
        insert(
            ScreenStateEntity.fromDomainModel(ScreenState(owner, JsonObject(previous + changes))),
        )
    }
}
