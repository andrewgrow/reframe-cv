package reframecv.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import reframecv.domain.models.screenstate.ScreenState
import reframecv.domain.models.screenstate.ScreenStateOwner

class TestScreenStatesRepository : ScreenStatesRepository {
    val states = MutableStateFlow<Map<ScreenStateOwner, JsonObject>>(emptyMap())
    override suspend fun get(owner: ScreenStateOwner) =
        states.value[owner]?.let { ScreenState(owner, it) }
    override suspend fun update(owner: ScreenStateOwner, changes: JsonObject) {
        states.update {
            it + (owner to JsonObject((it[owner] ?: JsonObject(emptyMap())) + changes))
        }
    }
}
