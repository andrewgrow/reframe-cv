package ankideckbuilder.domain.models.project

import ankideckbuilder.domain.DomainModel
import kotlin.time.Instant

data class Project(
    val id: Long = 0,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) : DomainModel
