package reframecv.database.resume

import androidx.room3.Embedded
import androidx.room3.Relation
import reframecv.database.DatabaseModel
import reframecv.domain.models.resume.Resume

/** A complete database record, including its separately indexed keywords. */
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
