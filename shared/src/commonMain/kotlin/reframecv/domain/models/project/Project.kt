package reframecv.domain.models.project

import reframecv.domain.DomainModel

/** Project timestamps are milliseconds since the Unix epoch. */
data class Project(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val parentId: Long? = null,
    val deletedAt: Long? = null,
    val mode: ProjectMode = ProjectMode.Unconfigured,
) : DomainModel
