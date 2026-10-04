package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.project.Project

interface ProjectsRepository {
    fun observeProjects(parentId: Long? = null): Flow<List<Project>>
    suspend fun createProject(name: String, parentId: Long? = null): Project
    suspend fun updateProject(id: Long, name: String)
    suspend fun deleteProject(id: Long)
}
