package reframecv.domain.models.project

import reframecv.domain.DomainModel

/** Groups job search work by role, company, or any structure the user chooses. */
data class Project(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val parentId: Long? = null,
    val deletedAt: Long? = null,
    val mode: ProjectMode = ProjectMode.Unconfigured,
    val mainResumeId: Long? = null,
) : DomainModel
