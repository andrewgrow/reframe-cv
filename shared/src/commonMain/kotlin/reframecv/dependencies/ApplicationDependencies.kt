package reframecv.dependencies

import reframecv.repository.AppSettingsRepository
import reframecv.repository.CoverLettersRepository
import reframecv.repository.ProjectsRepository
import reframecv.repository.ResumesRepository
import reframecv.repository.VacanciesRepository

/** Defines the contract each platform implements to provide application dependencies. */
interface ApplicationDependencies {
    val appSettingsRepository: AppSettingsRepository
    val projectsRepository: ProjectsRepository
    val resumesRepository: ResumesRepository
    val vacanciesRepository: VacanciesRepository
    val coverLettersRepository: CoverLettersRepository
    fun close()
}
