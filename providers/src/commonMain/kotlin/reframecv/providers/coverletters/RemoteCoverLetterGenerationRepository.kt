package reframecv.providers.coverletters

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import reframecv.coverletters.CoverLetterGenerationOutcome
import reframecv.coverletters.CoverLetterGenerationRepository
import reframecv.domain.models.providers.ProviderConnection
import reframecv.providers.AiProviderClient
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse
import reframecv.providers.AiTextDocument

/** Rejects malformed letter responses before they can create documents. */
class RemoteCoverLetterGenerationRepository(
    private val provider: AiProviderClient,
    private val schema: String,
) : CoverLetterGenerationRepository {
    override suspend fun generate(
        resumeJson: String,
        vacancyJson: String,
        connection: ProviderConnection,
        instruction: String,
    ): CoverLetterGenerationOutcome {
        val response = provider.execute(
            AiRequest(
                instruction,
                schema,
                emptyList(),
                connection,
                textDocuments = listOf(
                    AiTextDocument("resume.json", resumeJson),
                    AiTextDocument("vacancy.json", vacancyJson),
                ),
            ),
        )
        currentCoroutineContext().ensureActive()
        return when (response) {
            is AiResponse.Failed -> CoverLetterGenerationOutcome.Failed(
                response.details ?: "The provider could not generate the cover letter.",
            )

            is AiResponse.Generated -> decode(response.content)
        }
    }
    private fun decode(text: String): CoverLetterGenerationOutcome = try {
        val value = Json.parseToJsonElement(text) as? JsonObject
        require(value != null && value.keys == setOf("content"))
        val content = value["content"] as? JsonPrimitive
        require(content != null && content.isString && content.content.isNotBlank())
        CoverLetterGenerationOutcome.Generated(content.content)
    } catch (_: IllegalArgumentException) {
        CoverLetterGenerationOutcome.Failed(
            "The provider response does not match the cover letter schema.",
        )
    }
}
