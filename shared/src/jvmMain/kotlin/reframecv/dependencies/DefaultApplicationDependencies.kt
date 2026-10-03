package reframecv.dependencies

import reframecv.database.AppDatabase
import reframecv.database.openDefaultDatabase
import reframecv.repository.LocalProjectsRepository
import reframecv.repository.ProjectsRepository

class DefaultApplicationDependencies(databaseFactory: () -> AppDatabase = ::openDefaultDatabase) :
    ApplicationDependencies {
    private val database = lazy(databaseFactory)
    private val repository by lazy { LocalProjectsRepository(database.value.projectDao()) }
    private var closed = false

    override val projectsRepository: ProjectsRepository
        get() {
            check(!closed) { "Application dependencies are closed" }
            return repository
        }

    override fun close() {
        if (closed) return
        closed = true
        if (database.isInitialized()) database.value.close()
    }
}
