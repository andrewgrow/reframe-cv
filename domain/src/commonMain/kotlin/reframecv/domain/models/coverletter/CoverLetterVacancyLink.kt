package reframecv.domain.models.coverletter

import reframecv.domain.DomainModel

/** Identifies the linked vacancy shown beside a cover letter. */
data class CoverLetterVacancyLink(val letterId: Long, val vacancyId: Long, val name: String) :
    DomainModel
