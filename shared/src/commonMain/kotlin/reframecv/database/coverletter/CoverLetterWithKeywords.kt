package reframecv.database.coverletter

import androidx.room3.Embedded
import androidx.room3.Relation
import reframecv.database.DatabaseModel
import reframecv.domain.models.coverletter.CoverLetter

/** A complete database record, including its separately indexed keywords. */
data class CoverLetterWithKeywords(
    @Embedded
    val entity: CoverLetterEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["record_id"])
    val keywords: List<CoverLetterKeywordEntity>,
) : DatabaseModel<CoverLetter> {
    override fun toDomainModel() = entity.toDomainModel().copy(
        keywords = keywords.sortedBy { it.position }.map { it.keyword },
    )
}
