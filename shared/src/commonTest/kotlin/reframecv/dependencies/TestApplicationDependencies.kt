package reframecv.dependencies

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import reframecv.domain.models.project.Project
import reframecv.domain.models.project.ProjectMode
import reframecv.repository.ProjectsRepository
import reframecv.shared.nowMillis

class TestApplicationDependencies : ApplicationDependencies {
    override val projectsRepository = object : ProjectsRepository {
        private val projects = MutableStateFlow<List<Project>>(emptyList())
        override fun observeAllProjects() = projects.map { values ->
            values.filter { it.deletedAt == null }
        }

        override fun observeProjects(parentId: Long?) = projects.map { values ->
            values.filter { it.deletedAt == null && it.parentId == parentId }
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

        override suspend fun createProject(name: String, parentId: Long?): Project {
            require(name.isNotBlank())
            val parent = parentId?.let { id ->
                checkNotNull(projects.value.find { it.id == id && it.deletedAt == null })
                    .also { check(it.mode != ProjectMode.Workspace) }
            }
            val now = nowMillis()
            val project = Project(
                id = projects.value.size.toLong() + 1,
                name = name.trim(),
                parentId = parentId,
                createdAt = now,
                updatedAt = now,
            )
            projects.value = projects.value.map {
                if (it.id == parent?.id) {
                    it.copy(mode = ProjectMode.Container, updatedAt = now)
                } else {
                    it
                }
            } + project
            return project
        }
    }

    override fun close() = Unit
}
