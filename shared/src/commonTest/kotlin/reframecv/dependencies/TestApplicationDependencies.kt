package reframecv.dependencies

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import reframecv.domain.models.project.Project
import reframecv.repository.ProjectsRepository
import reframecv.shared.nowMillis

class TestApplicationDependencies : ApplicationDependencies {
    override val projectsRepository = object : ProjectsRepository {
        private val projects = MutableStateFlow<List<Project>>(emptyList())
        override fun observeProjects() = projects.map { values ->
            values.filter {
                it.deletedAt ==
                    null
            }
        }
        override suspend fun deleteProject(id: Long) {
            val ids = mutableSetOf(id)
            do {
                val added = projects.value.filter { it.parentId in ids }.map { it.id }
                val changed = ids.addAll(added)
            } while (changed)
            val deletedAt = nowMillis()
            projects.value = projects.value.map {
                if (it.id in ids && it.deletedAt == null) it.copy(deletedAt = deletedAt) else it
            }
        }
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
