package reframecv.ui.components.projects.resumes

import reframecv.ui.components.projects.resumes.editor.ResumeDraft

/** Lets resume UI refresh the list or submit a new structured draft. */
internal sealed interface ResumesIntent {
    data object Load : ResumesIntent

    /** Submits a new resume for the current project. */
    data class Create(val draft: ResumeDraft) : ResumesIntent
}

/** Lets the resume editor display progress and close only after a successful write. */
internal enum class ResumesLabel { Saving, Saved, SaveFailed }
