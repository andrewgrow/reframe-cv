package reframecv.dependencies

import kotlinx.coroutines.flow.MutableStateFlow
import reframecv.domain.models.project.Project
import reframecv.repository.ProjectsRepository
import reframecv.shared.nowMillis

class TestApplicationDependencies : ApplicationDependencies {
    override val projectsRepository = object : ProjectsRepository {
        private val projects = MutableStateFlow<List<Project>>(emptyList())
        override fun observeProjects() = projects
        override suspend fun updateProject(id: Long, name: String) {
            projects.value = projects.value.map { project ->
                if (project.id ==
                    id
                ) {
                    project.copy(name = name.trim(), updatedAt = nowMillis())
                } else {
                    project
                }
            }
        }

        override suspend fun createProject(name: String): Project {
            val now = nowMillis()
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
