package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.project.Project

interface ProjectsRepository {
    fun observeProjects(): Flow<List<Project>>
    suspend fun createProject(name: String): Project
    suspend fun updateProject(id: Long, name: String)
    suspend fun deleteProject(id: Long)
}
