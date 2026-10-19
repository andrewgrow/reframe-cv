package reframecv.database.screenstate

import androidx.room3.Query

/** Removes document-owned screen states together with the document. */
interface ScreenStateCleanupQueries {
    @Query("DELETE FROM screen_states WHERE screen = :screen AND document_id = :id")
    suspend fun clearScreenStates(screen: String, id: Long)
}
