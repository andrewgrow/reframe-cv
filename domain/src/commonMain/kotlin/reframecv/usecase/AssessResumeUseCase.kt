package reframecv.usecase

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.assessment.ResumeAssessment
import reframecv.assessment.ResumeAssessmentOutcome
import reframecv.assessment.ResumeAssessmentRepository
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.repository.VacanciesRepository
import reframecv.serialization.ResumeContentJson
import reframecv.serialization.VacancyContentJson
import reframecv.shared.nowMillis

/** Reports a safe assessment or persistence failure to the requesting screen. */
class ResumeAssessmentException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Assesses portable documents and immediately saves the assessed link for existing vacancies. */
class AssessResumeUseCase(
    private val repository: ResumeAssessmentRepository,
    private val providers: ProviderSettingsUseCases,
    private val vacancies: VacanciesRepository,
) {
    suspend operator fun invoke(
        resume: Resume,
        vacancy: Vacancy,
        connectionId: Long,
        instruction: String,
    ): ResumeAssessment {
        require(instruction.isNotBlank())
        require(
            resume.projectId == vacancy.projectId &&
                vacancy.resumes.any { it.resumeId == resume.id },
        )
        if (vacancy.description.isBlank() &&
            listOf(vacancy.requirements, vacancy.responsibilities, vacancy.niceToHave).all {
                it.isEmpty()
            }
        ) {
            throw ResumeAssessmentException("Add vacancy details before assessing a resume.")
        }
        val connection = providers.requireUsableConnection(connectionId)
        val vacancyJson = VacancyContentJson.encode(vacancy)
        val outcome = repository.assess(
            ResumeContentJson.encodeForExport(resume.content),
            vacancyJson,
            connection,
            instruction,
        )
        currentCoroutineContext().ensureActive()
        providers.requireUsableConnection(connectionId)
        val result = when (outcome) {
            is ResumeAssessmentOutcome.Assessed -> outcome.result
            is ResumeAssessmentOutcome.Failed -> throw ResumeAssessmentException(outcome.message)
        }
        val assessment = ResumeAssessment(
            result,
            nowMillis(),
            resume.updatedAt,
            vacancyJson,
            connection.name.ifBlank { connection.type.name },
            connection.openRouter.modelId,
        )
        if (vacancy.id != 0L) persist(vacancy, resume.id, assessment)
        return assessment
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun persist(vacancy: Vacancy, resumeId: Long, assessment: ResumeAssessment) {
        try {
            vacancies.saveResumeAssessment(vacancy.id, vacancy.projectId, resumeId, assessment)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw ResumeAssessmentException(
                "The assessment could not be saved. Please retry.",
                error,
            )
        }
    }
}
