package reframecv.providers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.OpenAiConfiguration
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection

class AiProviderRouterTest {
    @Test
    fun routesIdenticalOperationsOnlyToSelectedProviderAndRejectsNonAiConnections() = runBlocking {
        val calls = mutableListOf<Pair<ApiProviderType, AiRequest>>()
        val router = AiProviderRouter(
            listOf(ApiProviderType.OpenRouter, ApiProviderType.OpenAI).associateWith { type ->
                object : AiProviderClient {
                    override suspend fun execute(request: AiRequest): AiResponse {
                        calls += type to request
                        return AiResponse.Generated("{}")
                    }
                }
            },
        )
        val direct = ProviderConnection(
            type = ApiProviderType.OpenAI,
            name = "Direct",
            configuration = OpenAiConfiguration("key", "model"),
        )
        val request = AiRequest("Shared prompt", "{}", emptyList(), direct)
        assertEquals(AiResponse.Generated("{}"), router.execute(request))
        val routed = request.copy(
            connection = direct.copy(
                type = ApiProviderType.OpenRouter,
                configuration = OpenRouterConfiguration("key", "model"),
            ),
        )
        assertEquals(AiResponse.Generated("{}"), router.execute(routed))
        assertEquals(
            listOf(ApiProviderType.OpenAI to request, ApiProviderType.OpenRouter to routed),
            calls,
        )
        val invalid = listOf(
            direct.copy(active = false),
            direct.copy(type = ApiProviderType.Apify, configuration = ApifyConfiguration("key")),
            direct.copy(configuration = OpenAiConfiguration()),
        )
        invalid.forEach {
            assertEquals(
                AiResponse.Failed(AiProviderError.NotConfigured),
                router.execute(request.copy(connection = it)),
            )
        }
        assertEquals(2, calls.size)
    }
}
