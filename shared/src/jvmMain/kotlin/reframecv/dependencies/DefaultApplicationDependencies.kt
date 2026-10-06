package reframecv.dependencies

import reframecv.database.AppDatabase
import reframecv.database.openDefaultDatabase
import reframecv.repository.CoverLettersRepository
import reframecv.repository.LocalCoverLettersRepository
import reframecv.repository.LocalProjectsRepository
import reframecv.repository.LocalResumesRepository
import reframecv.repository.LocalVacanciesRepository
import reframecv.repository.ProjectsRepository
import reframecv.repository.ResumesRepository
import reframecv.repository.VacanciesRepository

class DefaultApplicationDependencies(databaseFactory: () -> AppDatabase = ::openDefaultDatabase) :
    ApplicationDependencies {
    private val database = lazy(databaseFactory)
    private val repository by lazy { LocalProjectsRepository(database.value.projectDao()) }
    private val resumeRepository by lazy { LocalResumesRepository(database.value.resumeDao()) }
    private val vacancyRepository by lazy { LocalVacanciesRepository(database.value.vacancyDao()) }
    private val coverLetterRepository by lazy {
        LocalCoverLettersRepository(database.value.coverLetterDao())
    }
    private var closed = false

    override val projectsRepository: ProjectsRepository
        get() {
            check(!closed) { "Application dependencies are closed" }
            return repository
        }

    override val resumesRepository: ResumesRepository
        get() {
            check(!closed) { "Application dependencies are closed" }
            return resumeRepository
        }

    override val vacanciesRepository: VacanciesRepository
        get() {
            check(!closed) { "Application dependencies are closed" }
            return vacancyRepository
        }

    override val coverLettersRepository: CoverLettersRepository
        get() {
            check(!closed) { "Application dependencies are closed" }
            return coverLetterRepository
        }

    override fun close() {
        if (closed) return
        closed = true
        if (database.isInitialized()) database.value.close()
    }
}
