package reframecv.dependencies

import reframecv.repository.ProjectsRepository

/** Shared application services; owned and closed by the application host. */
interface ApplicationDependencies {
    val projectsRepository: ProjectsRepository
    fun close()
}
