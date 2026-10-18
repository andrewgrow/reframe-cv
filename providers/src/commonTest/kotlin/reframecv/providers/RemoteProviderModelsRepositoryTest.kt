package reframecv.providers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.repository.ProviderSetupException

class RemoteProviderModelsRepositoryTest {
    @Test
    fun routesDiscoveryToTheRegisteredClientAndRejectsMissingImplementations() = runBlocking<Unit> {
        val models = listOf(AiModel("model", "Model"))
        val repository = RemoteProviderModelsRepository(
            mapOf(
                ApiProviderType.OpenRouter to ProviderSetupClient { key ->
                    assertEquals("example-key", key)
                    models
                },
            ),
        )
        assertEquals(models, repository.load(ApiProviderType.OpenRouter, "example-key"))
        assertFailsWith<ProviderSetupException> {
            RemoteProviderModelsRepository(
                emptyMap(),
            ).load(ApiProviderType.OpenRouter, "example-key")
        }
    }
}
