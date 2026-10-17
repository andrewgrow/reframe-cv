package reframecv.database.vacancy

import androidx.room3.Embedded
import androidx.room3.Relation
import reframecv.database.DatabaseModel
import reframecv.domain.models.vacancy.Vacancy

/** Lets features receive a vacancy with its keywords as one complete model. */
data class VacancyWithKeywords(
    @Embedded
    val entity: VacancyEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["record_id"])
    val keywords: List<VacancyKeywordEntity>,
) : DatabaseModel<Vacancy> {
    override fun toDomainModel() = entity.toDomainModel().copy(
        keywords = keywords.sortedBy { it.position }.map { it.keyword },
    )
}
