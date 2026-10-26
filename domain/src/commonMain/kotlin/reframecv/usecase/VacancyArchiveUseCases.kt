package reframecv.usecase

import kotlinx.coroutines.flow.first
import reframecv.domain.models.vacancy.Vacancy
import reframecv.export.VacancyArchive
import reframecv.export.VacancyArchiveChoices
import reframecv.export.VacancyArchiveExporter
import reframecv.export.archiveFilePrefix
import reframecv.repository.CoverLettersRepository
import reframecv.repository.ResumesRepository

/** Validates selected linked documents against fresh project data before archive generation. */
class VacancyArchiveUseCases(
    private val resumes: ResumesRepository,
    private val letters: CoverLettersRepository,
    private val exporter: VacancyArchiveExporter,
) {
    suspend fun choices(vacancy: Vacancy): VacancyArchiveChoices {
        require(vacancy.id > 0)
        val resumeIds = vacancy.resumes.map { it.resumeId }.toSet()
        return VacancyArchiveChoices(
            resumes.observeResumes(vacancy.projectId).first()
                .filter {
                    it.id in resumeIds && it.projectId == vacancy.projectId
                }.sortedBy { it.id },
            letters.observeCoverLetters(vacancy.projectId).first()
                .filter { it.id in vacancy.coverLetterIds && it.projectId == vacancy.projectId }
                .sortedBy { it.id },
        )
    }
    suspend fun export(
        vacancy: Vacancy,
        name: String,
        resumeIds: Set<Long>,
        letterIds: Set<Long>,
    ): reframecv.export.DocumentExportResult {
        val prefix = archiveFilePrefix(name)
        require(prefix.isNotBlank()) { "Archive name must not be blank" }
        val available = choices(vacancy)
        require(resumeIds.all { id -> available.resumes.any { it.id == id } })
        require(letterIds.all { id -> available.coverLetters.any { it.id == id } })
        return exporter.export(
            VacancyArchive(
                vacancy,
                prefix,
                available.resumes.filter { it.id in resumeIds },
                available.coverLetters.filter { it.id in letterIds },
            ),
        )
    }
}
