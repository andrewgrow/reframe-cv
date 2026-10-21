package reframecv.domain.models.coverletter

import reframecv.domain.DomainModel

/** Identifies the source resume shown beside a cover letter. */
data class CoverLetterResumeLink(val letterId: Long, val resumeId: Long, val name: String) :
    DomainModel
