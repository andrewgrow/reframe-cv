package reframecv.repository

import kotlinx.serialization.json.JsonObject
import reframecv.domain.models.screenstate.ScreenState
import reframecv.domain.models.screenstate.ScreenStateOwner

/** Persists screen-specific JSON fields independently of document content. */
interface ScreenStatesRepository {
    suspend fun get(owner: ScreenStateOwner): ScreenState?
    suspend fun update(owner: ScreenStateOwner, changes: JsonObject)
}
