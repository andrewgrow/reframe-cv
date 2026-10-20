package reframecv.usecase

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.improvement.ResumeImprovementOutcome
import reframecv.improvement.ResumeImprovementRepository
import reframecv.repository.ResumesRepository
import reframecv.serialization.ResumeContentJson
import reframecv.serialization.VacancyContentJson
import reframecv.shared.nowMillis

/** Reports an improvement or atomic save failure without dismissing the request dialog. */
class ResumeImprovementException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Creates an independent AI adaptation and links it without saving other vacancy draft changes. */
class ImproveResumeUseCase(
    private val repository: ResumeImprovementRepository,
    private val providers: ProviderSettingsUseCases,
    private val resumes: ResumesRepository,
    private val timestamp: () -> Long = ::nowMillis,
) {
    suspend operator fun invoke(
        source: Resume,
        vacancy: Vacancy,
        connectionId: Long,
        instruction: String,
    ): Resume {
        require(instruction.isNotBlank())
        require(source.id != 0L && source.projectId == vacancy.projectId)
        require(vacancy.resumes.any { it.resumeId == source.id })
        if (vacancy.description.isBlank() &&
            listOf(vacancy.requirements, vacancy.responsibilities, vacancy.niceToHave).all {
                it.isEmpty()
            }
        ) {
            throw ResumeImprovementException("Add vacancy details before improving a resume.")
        }
        val connection = providers.requireUsableConnection(connectionId)
        val outcome = repository.improve(
            ResumeContentJson.encodeForExport(source.content),
            VacancyContentJson.encode(vacancy),
            connection,
            instruction,
        )
        currentCoroutineContext().ensureActive()
        providers.requireUsableConnection(connectionId)
        val content = when (outcome) {
            is ResumeImprovementOutcome.Improved -> outcome.content
            is ResumeImprovementOutcome.Failed -> throw ResumeImprovementException(outcome.message)
        }
        val time = timestamp()
        val record = Resume(
            projectId = source.projectId,
            name = "${source.name} (AI ${improvementTimestamp(time)})",
            createdAt = time,
            updatedAt = time,
            content = content,
            sourceResumeId = source.id,
        )
        return persist(record, vacancy.id.takeIf { it != 0L })
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun persist(record: Resume, vacancyId: Long?): Resume = try {
        resumes.createAdaptation(record, vacancyId)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        throw ResumeImprovementException(
            "The improved resume could not be saved. Please retry.",
            error,
        )
    }
}

private fun improvementTimestamp(millis: Long): String {
    val time = Instant.fromEpochMilliseconds(
        millis,
    ).toLocalDateTime(TimeZone.currentSystemDefault())
    fun Int.twoDigits() = toString().padStart(2, '0')
    return "${time.year}-${time.day.twoDigits()}-${(time.month.ordinal + 1).twoDigits()} " +
        "${time.hour.twoDigits()}:${time.minute.twoDigits()}"
}
