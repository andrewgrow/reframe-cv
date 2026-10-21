package reframecv.domain.models.resume

import reframecv.domain.DomainModel

/** Identifies a vacancy linked to a resume without loading its full document. */
data class ResumeVacancyLink(val resumeId: Long, val vacancyId: Long, val name: String) :
    DomainModel
