package reframecv.database.resume

import androidx.room3.Embedded
import androidx.room3.Relation
import reframecv.database.DatabaseModel
import reframecv.domain.models.resume.Resume

/** Lets features receive a resume with its keywords as one complete model. */
data class ResumeWithKeywords(
    @Embedded
    val entity: ResumeEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["record_id"])
    val keywords: List<ResumeKeywordEntity>,
) : DatabaseModel<Resume> {
    override fun toDomainModel() = entity.toDomainModel().copy(
        keywords = keywords.sortedBy { it.position }.map { it.keyword },
    )
}
