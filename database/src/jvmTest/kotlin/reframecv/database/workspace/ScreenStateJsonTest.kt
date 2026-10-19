package reframecv.database.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.database.screenstate.ScreenStateEntity
import reframecv.domain.models.screenstate.ScreenState
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.repository.LocalScreenStatesRepository

class ScreenStateJsonTest : WorkspaceDatabaseTest() {
    @Test
    fun corruptOrNonObjectJsonFallsBackAndCanBeRepaired() = runBlocking {
        val owner = ScreenStateOwner(ScreenType.Settings)
        val dao = database.screenStatesDao()
        val repository = LocalScreenStatesRepository(dao)
        val row = ScreenStateEntity.fromDomainModel(ScreenState(owner))
        for (raw in listOf("{broken", "[]", "null", "42", "\"text\"")) {
            dao.insert(row.copy(states = raw))
            assertEquals(ScreenState(owner), repository.get(owner))
            repository.update(owner, buildJsonObject { put("job_details", "collapsed") })
            assertEquals("collapsed", repository.get(owner)?.text("job_details"))
        }
    }

    @Test
    fun concurrentPatchesKeepUnknownAndInvalidFieldsUntilExplicitlyReplaced() = runBlocking {
        val owner = ScreenStateOwner(ScreenType.Settings)
        val repository = LocalScreenStatesRepository(database.screenStatesDao())
        val initial = Json.parseToJsonElement(
            """{"job_details":42,"selected_tab":"attachments","unknown":{"items":[1,true,null]}}""",
        ).let { it as kotlinx.serialization.json.JsonObject }
        repository.update(owner, initial)
        assertEquals(null, repository.get(owner)?.text("job_details"))
        assertEquals("attachments", repository.get(owner)?.text("selected_tab"))
        coroutineScope {
            (1..10).map { index ->
                async(Dispatchers.Default) {
                    repository.update(owner, buildJsonObject { put("field_$index", index) })
                }
            }.awaitAll()
        }
        repository.update(owner, buildJsonObject { put("job_details", "expanded") })
        val restored = repository.get(owner)!!
        assertEquals("expanded", restored.text("job_details"))
        assertEquals(initial["unknown"], restored.states["unknown"])
        assertEquals("attachments", restored.text("selected_tab"))
        assertEquals(13, restored.states.size)
    }
}
