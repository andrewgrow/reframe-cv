package reframecv.dependencies

import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import reframecv.domain.models.project.Project
import reframecv.repository.ProjectsRepository

class TestApplicationDependencies : ApplicationDependencies {
    override val projectsRepository = object : ProjectsRepository {
        private val projects = MutableStateFlow<List<Project>>(emptyList())
        override fun observeProjects() = projects
        override suspend fun createProject(name: String): Project {
            val now = Clock.System.now()
            val project = Project(
                id = projects.value.size.toLong() + 1,
                name = name.trim(),
                createdAt = now,
                updatedAt = now,
            )
            projects.value += project
            return project
        }
    }
    override fun close() = Unit
}
