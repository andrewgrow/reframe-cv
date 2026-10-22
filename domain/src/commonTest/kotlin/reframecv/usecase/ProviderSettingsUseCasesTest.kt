package reframecv.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection
import reframecv.repository.ProviderAccountRepository
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.ProviderModelsRepository
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException

class ProviderSettingsUseCasesTest {
    private val connection =
        ProviderConnection(
            id = 7,
            type = ApiProviderType.OpenRouter,
            name = "",
            configuration = OpenRouterConfiguration(
                apiKey = "valid-key",
                modelId = "model",
                managementApiKey = "management-key",
            ),
        )
    private var stored = connection
    private var writes = 0
    private val repository = object : ProviderConnectionsRepository {
        override fun observe() = MutableStateFlow(listOf(stored))
        override suspend fun get(id: Long) = stored.takeIf { it.id == id }
        override suspend fun save(connection: ProviderConnection) = connection.id
        override suspend fun setActive(id: Long, active: Boolean) {
            stored =
                stored.copy(active = active)
        }
        override suspend fun updateCreditBalance(
            id: Long,
            type: ApiProviderType,
            managementApiKey: String,
            balance: reframecv.domain.models.providers.ProviderCreditBalance,
        ) = Unit
        override suspend fun updateAccountInfo(
            id: Long,
            type: ApiProviderType,
            apiKey: String,
            info: ProviderAccountInfo,
        ) {
            writes++
        }
    }

    @Test
    fun disabledDraftsAndStoredConnectionsNeverReachTheProvider() = runBlocking<Unit> {
        var requests = 0
        val useCases = ProviderSettingsUseCases(
            repository,
            ProviderModelsRepository { _, _ ->
                requests++
                emptyList()
            },
            ProviderAccountRepository { _, _ ->
                requests++
                ProviderAccountInfo(fetchedAt = 1)
            },
        )
        val drafts = listOf(connection.copy(id = 0, active = false), connection)
        stored = stored.copy(active = false)
        drafts.forEach { draft ->
            assertEquals(
                ProviderSetupError.Disabled,
                assertFailsWith<ProviderSetupException> {
                    useCases.loadModels(draft)
                }.reason,
            )
            assertEquals(
                ProviderSetupError.Disabled,
                assertFailsWith<ProviderSetupException> {
                    useCases.refreshAccountInfo(draft)
                }.reason,
            )
            assertEquals(
                ProviderSetupError.Disabled,
                assertFailsWith<ProviderSetupException> {
                    useCases.refreshCreditBalance(draft)
                }.reason,
            )
        }
        assertEquals(0, requests)
        assertEquals(0, writes)
    }

    @Test
    fun disablingDuringAnOperationDiscardsItsResultsBeforeTheyCanBeUsedOrPersisted() =
        runBlocking<Unit> {
            val useCases = ProviderSettingsUseCases(
                repository,
                ProviderModelsRepository { _, _ ->
                    stored = stored.copy(active = false)
                    listOf(AiModel("model", "Model"))
                },
                object : ProviderAccountRepository {
                    override suspend fun load(
                        type: ApiProviderType,
                        apiKey: String,
                    ): ProviderAccountInfo {
                        stored = stored.copy(active = false)
                        return ProviderAccountInfo(fetchedAt = 1)
                    }
                    override suspend fun balance(
                        type: ApiProviderType,
                        managementApiKey: String,
                    ): reframecv.domain.models.providers.ProviderCreditBalance {
                        stored = stored.copy(active = false)
                        return reframecv.domain.models.providers.ProviderCreditBalance(
                            50.0,
                            1.0,
                            123,
                        )
                    }
                },
            )
            assertFailsWith<ProviderSetupException> { useCases.loadModels(connection) }
            stored = stored.copy(active = true)
            assertFailsWith<ProviderSetupException> { useCases.refreshAccountInfo(connection) }
            stored = stored.copy(active = true)
            assertFailsWith<ProviderSetupException> { useCases.refreshCreditBalance(connection) }
            assertEquals(0, writes)
        }
}
