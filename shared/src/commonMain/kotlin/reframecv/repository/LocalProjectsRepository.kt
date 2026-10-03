package reframecv.repository

import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import reframecv.database.project.ProjectDao
import reframecv.database.project.ProjectEntity
import reframecv.domain.models.project.Project

class LocalProjectsRepository(private val projectDao: ProjectDao) : ProjectsRepository {
    override fun observeProjects(): Flow<List<Project>> = projectDao.observeAll()
        .map { entities -> entities.map(ProjectEntity::toDomainModel) }

    override suspend fun createProject(name: String): Project {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Project name must not be blank" }
        val now = Clock.System.now()
        val project = Project(name = trimmedName, createdAt = now, updatedAt = now)
        val entity = ProjectEntity.fromDomainModel(project)
        val id = projectDao.insert(entity)
        return entity.copy(id = id).toDomainModel()
    }
}
