package reframecv.providers.assessment

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.assessment.ResumeAssessmentJson
import reframecv.assessment.ResumeAssessmentOutcome
import reframecv.assessment.ResumeAssessmentRepository
import reframecv.domain.models.providers.ProviderConnection
import reframecv.providers.AiProviderClient
import reframecv.providers.AiRequest
import reframecv.providers.AiResponse
import reframecv.providers.AiTextDocument

/** Adapts structured provider output to validated vacancy-specific resume assessments. */
class RemoteResumeAssessmentRepository(
    private val provider: AiProviderClient,
    private val responseSchema: String,
) : ResumeAssessmentRepository {
    override suspend fun assess(
        resumeJson: String,
        vacancyJson: String,
        connection: ProviderConnection,
        instruction: String,
    ): ResumeAssessmentOutcome {
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
            is AiResponse.Failed -> ResumeAssessmentOutcome.Failed(
                response.details ?: "The provider could not complete the assessment.",
            )

            is AiResponse.Generated -> decode(response.content)
        }
    }

    private fun decode(content: String): ResumeAssessmentOutcome = try {
        ResumeAssessmentOutcome.Assessed(ResumeAssessmentJson.decodeResult(content))
    } catch (error: CancellationException) {
        throw error
    } catch (_: IllegalArgumentException) {
        ResumeAssessmentOutcome.Failed(
            "The provider response does not match the resume assessment schema.",
        )
    }
}
