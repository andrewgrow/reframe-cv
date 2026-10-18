package reframecv.providers

import reframecv.domain.models.providers.ProviderConnection

/** Supplies an original document without exposing device paths to an AI provider. */
class AiDocument(val fileName: String, val mediaType: String, val bytes: ByteArray)

/** Describes a structured generation task independently of a provider's request format. */
data class AiRequest(
    val instruction: String,
    val responseSchema: String,
    val documents: List<AiDocument>,
    val connection: ProviderConnection,
)

/** Classifies provider failures without exposing HTTP or SDK types to application code. */
enum class AiProviderError { NotConfigured, Unavailable, InvalidResponse }

/** Carries generated JSON or a provider failure before business-specific decoding. */
sealed interface AiResponse {
    data class Generated(val content: String) : AiResponse
    data class Failed(val error: AiProviderError, val details: String? = null) : AiResponse
}

/** Defines the provider operations used by external repositories. */
interface AiProviderClient {
    suspend fun execute(request: AiRequest): AiResponse
    fun close() = Unit
}
