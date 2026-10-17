package reframecv.usecase

import reframecv.repository.ProjectsRepository

/** Provides project browsing and editing actions without exposing storage to components. */
class ProjectsUseCases(private val repository: ProjectsRepository) {
    fun observeAll() = repository.observeAllProjects()
    fun observeChildren(parentId: Long?) = repository.observeProjects(parentId)

    suspend fun create(name: String, parentId: Long?) = repository.createProject(
        validatedName(name),
        parentId,
    )

    suspend fun rename(id: Long, name: String) = repository.updateProject(id, validatedName(name))
    suspend fun delete(id: Long) = repository.deleteProject(id)

    private fun validatedName(name: String) = name.trim().also {
        require(it.isNotEmpty()) { "Project name must not be blank" }
    }
}
