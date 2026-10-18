package reframecv.database.vacancy

import androidx.room3.Embedded
import androidx.room3.Relation
import reframecv.database.DatabaseModel
import reframecv.domain.models.vacancy.Vacancy

/** Loads vacancy fields, keywords, and resume links as one complete model. */
data class VacancyWithKeywords(
    @Embedded
    val entity: VacancyEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["record_id"])
    val keywords: List<VacancyKeywordEntity>,
    @Relation(parentColumns = ["id"], entityColumns = ["vacancy_id"])
    val resumes: List<VacancyResumeEntity> = emptyList(),
) : DatabaseModel<Vacancy> {
    override fun toDomainModel() = entity.toDomainModel().copy(
        resumes = resumes.sortedBy { it.position }.map { it.toDomainModel() },
        keywords = keywords.sortedBy { it.position }.map { it.keyword },
    )
}
