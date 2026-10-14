package reframecv.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import reframecv.database.project.ProjectDao
import reframecv.database.project.ProjectEntity
import reframecv.domain.models.project.Project
import reframecv.shared.nowMillis

/** Keeps project organization in local storage with permanent subtree deletion. */
class LocalProjectsRepository(
    private val projectDao: ProjectDao,
    private val snapshots: VacancySnapshotManager? = null,
    private val photos: LocalResumePhotos? = null,
) : ProjectsRepository {
    override suspend fun deleteProject(id: Long) {
        val remove = suspend {
            snapshots?.deleteProject(projectDao, id)
                ?: projectDao.deleteSubtree(id)
        }
        val deletedRows = photos?.deleteProject(projectDao, id, remove) ?: remove()
        check(deletedRows > 0) { "Project does not exist" }
    }

    override suspend fun updateProject(id: Long, name: String) {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Project name must not be blank" }
        val updatedAt = nowMillis()
        val updatedRows = projectDao.updateName(id, trimmedName, updatedAt)
        check(updatedRows == 1) { "Project does not exist" }
    }

    override fun observeAllProjects(): Flow<List<Project>> = projectDao.observeAll()
        .map { entities -> entities.map(ProjectEntity::toDomainModel) }

    override fun observeProjects(parentId: Long?): Flow<List<Project>> =
        projectDao.observeChildren(parentId)
            .map { entities -> entities.map(ProjectEntity::toDomainModel) }

    override suspend fun createProject(name: String, parentId: Long?): Project {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Project name must not be blank" }
        val now = nowMillis()
        val project =
            Project(name = trimmedName, createdAt = now, updatedAt = now, parentId = parentId)
        val entity = ProjectEntity.fromDomainModel(project)
        val id = projectDao.insertWithParentMode(entity)
        return entity.copy(id = id).toDomainModel()
    }
}
