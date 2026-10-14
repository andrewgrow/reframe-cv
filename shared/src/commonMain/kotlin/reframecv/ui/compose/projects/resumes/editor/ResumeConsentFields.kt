package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_consent_statement
import reframecv.shared.generated.resources.resume_text
import reframecv.shared.generated.resources.resume_visible
import reframecv.ui.components.projects.resumes.editor.ResumeDraft

@Composable
internal fun ResumeFormScope.ConsentFields(draft: ResumeDraft, onChange: (ResumeDraft) -> Unit) {
    Heading(Res.string.resume_consent_statement)
    Column(Modifier.testTag("resume.consent.visibility")) {
        Toggle(draft.consent.visible, Res.string.resume_visible) {
            onChange(draft.copy(consent = draft.consent.copy(visible = it)))
        }
    }
    Field(draft.consent.text, Res.string.resume_text, {
        onChange(draft.copy(consent = draft.consent.copy(text = it)))
    }, multiline = true, tag = "resume.consent")
}
