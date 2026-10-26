package reframecv.ui.components.projects.vacancies.editor

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.export.archiveFilePrefix

/** Keeps archive naming and document choices separate from the saved vacancy. */
data class VacancyArchiveDraft(
    val name: String = "",
    val resumes: List<Resume> = emptyList(),
    val letters: List<CoverLetter> = emptyList(),
    val resumeIds: Set<Long> = emptySet(),
    val letterIds: Set<Long> = emptySet(),
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val failed: Boolean = false,
    val invalidName: Boolean = false,
) {
    val filePrefix: String get() = archiveFilePrefix(name)
    val ready: Boolean get() = loaded && !loading && !failed && filePrefix.isNotBlank()
}
