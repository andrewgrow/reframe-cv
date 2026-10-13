package reframecv.ui.components.projects.resumes

import reframecv.domain.models.resume.Resume
import reframecv.ui.components.projects.resumes.editor.ResumeDraft

/** Lets resume UI refresh the list and submit structured resume changes. */
internal sealed interface ResumesIntent {
    data object Load : ResumesIntent

    /** Submits a new resume for the current project. */
    data class Create(val draft: ResumeDraft) : ResumesIntent

    /** Submits changes while retaining the saved resume's identity and provenance. */
    data class Update(val resume: Resume, val draft: ResumeDraft) : ResumesIntent
}

/** Lets the resume editor display progress and close only after a successful write. */
internal enum class ResumesLabel { Saving, Saved, SaveFailed }
