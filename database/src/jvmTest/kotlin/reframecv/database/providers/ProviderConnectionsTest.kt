package reframecv.database.providers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.repository.LocalProviderConnectionsRepository

class ProviderConnectionsTest : WorkspaceDatabaseTest() {
    @Test
    fun startsEmptyAndPersistsConnectionsWithExactlyOneActiveSelection() = runBlocking {
        var repository = LocalProviderConnectionsRepository(database.providerConnectionsDao())
        assertEquals(emptyList(), repository.observe().first())
        val draft =
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Work",
                apiKey = "example-key",
                modelId = "model-a",
            )
        val first = repository.save(draft)
        val second = repository.save(draft.copy(name = "Personal", modelId = "model-b"))
        assertEquals(listOf(first), repository.observe().first().filter { it.active }.map { it.id })
        repository.activate(second)
        val created = repository.observe().first().last()
        repository.save(created.copy(name = "Edited", apiKey = "replacement", active = false))
        assertFailsWith<IllegalStateException> { repository.activate(-1) }
        database.close()
        openDatabase()
        repository = LocalProviderConnectionsRepository(database.providerConnectionsDao())
        val rows = repository.observe().first()
        assertEquals(listOf(first, second), rows.map { it.id })
        assertFalse(rows.first().active)
        assertTrue(rows.last().active)
        assertEquals("replacement", rows.last().apiKey)
        assertEquals(created.createdAt, rows.last().createdAt)
        assertFalse(rows.last().toString().contains("replacement"))
    }
}
