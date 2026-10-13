package reframecv.ui.components.projects.resumes

import reframecv.domain.models.resume.Resume
import reframecv.repository.MainResumeChange
import reframecv.ui.components.projects.resumes.editor.ResumeDraft

/** Lets resume UI refresh the list and submit structured resume changes. */
internal sealed interface ResumesIntent {
    data object Load : ResumesIntent

    /** Submits a new resume for the current project. */
    data class Create(
        val draft: ResumeDraft,
        val mainResumeChange: MainResumeChange = MainResumeChange.Keep,
    ) : ResumesIntent

    /** Permanently removes the selected resume and its owned data. */
    data class Delete(val id: Long) : ResumesIntent

    /** Submits changes while retaining the saved resume's identity and provenance. */
    data class Update(
        val resume: Resume,
        val draft: ResumeDraft,
        val mainResumeChange: MainResumeChange = MainResumeChange.Keep,
    ) : ResumesIntent
}

/** Lets the resume editor display progress and close only after a successful write. */
internal enum class ResumesLabel { Saving, Saved, SaveFailed, Deleting, Deleted, DeleteFailed }
