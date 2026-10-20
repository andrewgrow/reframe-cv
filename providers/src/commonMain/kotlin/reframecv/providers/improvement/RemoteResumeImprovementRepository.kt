package reframecv.providers.improvement

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.domain.models.providers.ProviderConnection
import reframecv.improvement.ResumeImprovementOutcome
import reframecv.improvement.ResumeImprovementRepository
import reframecv.providers.AiProviderClient
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse
import reframecv.providers.AiTextDocument
import reframecv.serialization.ResumeContentJson

/** Decodes AI adaptations strictly, preserving the source resume on malformed responses. */
class RemoteResumeImprovementRepository(
    private val provider: AiProviderClient,
    private val responseSchema: String,
) : ResumeImprovementRepository {
    override suspend fun improve(
        resumeJson: String,
        vacancyJson: String,
        connection: ProviderConnection,
        instruction: String,
    ): ResumeImprovementOutcome {
        val response = provider.execute(
            AiRequest(
                instruction,
                responseSchema,
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
            is AiResponse.Failed -> ResumeImprovementOutcome.Failed(
                response.details ?: "The provider could not improve the resume.",
            )

            is AiResponse.Generated -> decode(response.content)
        }
    }

    private fun decode(value: String): ResumeImprovementOutcome = try {
        ResumeImprovementOutcome.Improved(ResumeContentJson.decodeGenerated(value))
    } catch (error: CancellationException) {
        throw error
    } catch (_: IllegalArgumentException) {
        ResumeImprovementOutcome.Failed("The provider response does not match the resume schema.")
    }
}
