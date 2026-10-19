package reframecv.repository

import kotlinx.serialization.json.JsonObject
import reframecv.database.screenstate.ScreenStatesDao
import reframecv.domain.models.screenstate.ScreenStateOwner

/** Restores generic screen state and applies field-level JSON patches locally. */
class LocalScreenStatesRepository(private val dao: ScreenStatesDao) : ScreenStatesRepository {
    override suspend fun get(owner: ScreenStateOwner) =
        dao.get(owner.screen.name, owner.documentId, owner.projectId)?.toDomainModel()
    override suspend fun update(owner: ScreenStateOwner, changes: JsonObject) =
        dao.update(owner, changes)
}
