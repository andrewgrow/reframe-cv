package reframecv.domain.models.project

import kotlin.time.Instant
import reframecv.domain.DomainModel

data class Project(
    val id: Long = 0,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val parentId: Long? = null,
) : DomainModel
