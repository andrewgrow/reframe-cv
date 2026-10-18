package reframecv.database.providers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderBudget
import reframecv.domain.models.providers.ProviderConnection
import reframecv.repository.LocalProviderConnectionsRepository

class ProviderConnectionsTest : WorkspaceDatabaseTest() {
    @Test
    fun cachesAccountInfoAcrossReopensWithoutStaleDraftsOrOldKeysOverwritingIt() = runBlocking {
        var repository = LocalProviderConnectionsRepository(database.providerConnectionsDao())
        val id = repository.save(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Work",
                apiKey = "old",
                modelId = "model",
            ),
        )
        val original = repository.observe().first().single()
        val info =
            ProviderAccountInfo(
                listOf(ProviderBudget("key_limit", 50.0, 1.0, 49.0)),
                expiresAt = 42,
                fetchedAt = 123,
            )
        repository.updateAccountInfo(id, original.type, "wrong", info)
        assertNull(repository.observe().first().single().accountInfo)
        repository.updateAccountInfo(id, original.type, "old", info)
        assertEquals(original.updatedAt, repository.observe().first().single().updatedAt)
        repository.save(original.copy(name = "Edited"))
        assertEquals(info, repository.observe().first().single().accountInfo)
        database.close()
        openDatabase()
        repository = LocalProviderConnectionsRepository(database.providerConnectionsDao())
        assertEquals(info, repository.observe().first().single().accountInfo)

        repository.setActive(id, false)
        repository.updateAccountInfo(id, original.type, "old", info.copy(fetchedAt = 999))
        assertEquals(info, repository.get(id)?.accountInfo)
        repository.setActive(id, true)
        val refreshed = repository.observe().first().single()
        repository.save(refreshed.copy(apiKey = "new"))
        assertNull(repository.observe().first().single().accountInfo)
        repository.updateAccountInfo(id, original.type, "old", info)
        assertNull(repository.observe().first().single().accountInfo)
        repository.updateAccountInfo(id, original.type, "new", info.copy(fetchedAt = 456))
        assertEquals(456L, repository.observe().first().single().accountInfo?.fetchedAt)
    }

    @Test
    fun balanceCacheUsesItsOwnCredentialAndSurvivesReopensAndUnrelatedEdits() = runBlocking {
        var repository = LocalProviderConnectionsRepository(database.providerConnectionsDao())
        val id = repository.save(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "",
                apiKey = "ordinary",
                modelId = "",
                managementApiKey = "management",
            ),
        )
        val original = repository.get(id)!!
        val balance = reframecv.domain.models.providers.ProviderCreditBalance(100.0, 25.0, 123)
        repository.updateCreditBalance(id, original.type, "wrong-key", balance)
        assertNull(repository.get(id)?.creditBalance)
        repository.updateCreditBalance(id, original.type, "management", balance)
        repository.save(original.copy(name = "Edited", apiKey = "changed-ordinary"))
        assertEquals(balance, repository.get(id)?.creditBalance)
        database.close()
        openDatabase()
        repository = LocalProviderConnectionsRepository(database.providerConnectionsDao())
        assertEquals("management", repository.get(id)?.managementApiKey)
        assertEquals(balance, repository.get(id)?.creditBalance)
        repository.setActive(id, false)
        repository.updateCreditBalance(
            id,
            original.type,
            "management",
            balance.copy(fetchedAt = 456),
        )
        assertEquals(balance, repository.get(id)?.creditBalance)
        repository.save(repository.get(id)!!.copy(managementApiKey = "new-management"))
        assertNull(repository.get(id)?.creditBalance)
        repository.setActive(id, true)
        repository.updateCreditBalance(id, original.type, "management", balance)
        assertNull(repository.get(id)?.creditBalance)
        repository.updateCreditBalance(id, original.type, "new-management", balance)
        assertEquals(balance, repository.get(id)?.creditBalance)
        assertFalse(repository.get(id).toString().contains("new-management"))
    }

    @Test
    fun persistsIndependentEnabledStatesAndPreservesThemWhenSavingDrafts() = runBlocking {
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
        assertEquals(
            listOf(first, second),
            repository.observe().first().filter {
                it.active
            }.map { it.id },
        )
        repository.setActive(first, false)
        val created = repository.observe().first().last()
        repository.save(created.copy(name = "Edited", apiKey = "replacement", active = false))
        assertFailsWith<IllegalStateException> { repository.setActive(-1, true) }
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
