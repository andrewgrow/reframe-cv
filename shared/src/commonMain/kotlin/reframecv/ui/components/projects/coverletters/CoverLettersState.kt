package reframecv.ui.components.projects.coverletters

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.coverletter.CoverLetterResumeLink
import reframecv.domain.models.resume.Resume

/** Provides loading feedback and the current project's letters with their source resumes. */
sealed interface CoverLettersState {
    data object Loading : CoverLettersState
    data object LoadFailed : CoverLettersState

    /** Retains source records so links always open the latest resume. */
    data class Ready(val letters: List<CoverLetter>, val resumes: List<Resume> = emptyList()) :
        CoverLettersState {
        val linkedResumes: List<CoverLetterResumeLink> get() = letters.mapNotNull { letter ->
            resumes.find { it.id == letter.sourceResumeId }?.let {
                CoverLetterResumeLink(letter.id, it.id, it.name)
            }
        }
    }
}
