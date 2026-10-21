package reframecv.usecase

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.coverletters.CoverLetterGenerationOutcome
import reframecv.coverletters.CoverLetterGenerationRepository
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.repository.CoverLettersRepository
import reframecv.serialization.ResumeContentJson
import reframecv.serialization.VacancyContentJson
import reframecv.shared.nowMillis

/** Reports generation or persistence failures without dismissing the AI dialog. */
class CoverLetterGenerationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Creates a reviewable letter and atomically attaches it to an existing vacancy. */
class GenerateCoverLetterUseCase(
    private val remote: CoverLetterGenerationRepository,
    private val providers: ProviderSettingsUseCases,
    private val letters: CoverLettersRepository,
    private val timestamp: () -> Long = ::nowMillis,
) {
    suspend operator fun invoke(
        source: Resume,
        vacancy: Vacancy,
        connectionId: Long,
        instruction: String,
    ): CoverLetter {
        require(instruction.isNotBlank())
        require(source.id > 0 && source.projectId == vacancy.projectId)
        require(vacancy.resumes.any { it.resumeId == source.id })
        val connection = providers.requireUsableConnection(connectionId)
        val result = remote.generate(
            ResumeContentJson.encodeForExport(source.content),
            VacancyContentJson.encode(vacancy),
            connection,
            instruction,
        )
        currentCoroutineContext().ensureActive()
        providers.requireUsableConnection(connectionId)
        val text = when (result) {
            is CoverLetterGenerationOutcome.Generated -> result.content.trim()

            is CoverLetterGenerationOutcome.Failed -> throw CoverLetterGenerationException(
                result.message,
            )
        }
        if (text.isBlank()) {
            throw CoverLetterGenerationException(
                "The provider returned an empty cover letter.",
            )
        }
        val time = timestamp()
        val letter =
            CoverLetter(
                projectId = source.projectId,
                name = "Cover letter: ${improvementName(source.name, vacancy.name, time)}",
                createdAt = time,
                updatedAt = time,
                content = text,
                sourceResumeId = source.id,
            )
        return persist(letter, vacancy.id.takeIf { it > 0 })
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun persist(letter: CoverLetter, vacancyId: Long?): CoverLetter = try {
        letters.createAndLink(letter, vacancyId)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        throw CoverLetterGenerationException(
            "The cover letter could not be saved. Please retry.",
            error,
        )
    }
}
