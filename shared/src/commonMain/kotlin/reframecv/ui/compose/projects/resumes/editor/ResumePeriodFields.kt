package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_current
import reframecv.shared.generated.resources.resume_date_error
import reframecv.shared.generated.resources.resume_end
import reframecv.shared.generated.resources.resume_start
import reframecv.ui.components.projects.resumes.editor.ResumePeriodDraft

@Composable
internal fun ResumeFormScope.PeriodFields(
    period: ResumePeriodDraft,
    onChange: (ResumePeriodDraft) -> Unit,
) {
    val error = stringResource(Res.string.resume_date_error)
    Field(
        period.start,
        Res.string.resume_start,
        { onChange(period.copy(start = it)) },
        error = if (ResumePeriodDraft.validDate(period.start)) null else error,
    )
    Toggle(period.isCurrent, Res.string.resume_current) { onChange(period.copy(isCurrent = it)) }
    if (!period.isCurrent) {
        Field(
            period.end,
            Res.string.resume_end,
            { onChange(period.copy(end = it)) },
            error = if (ResumePeriodDraft.validDate(period.end)) null else error,
        )
    }
}
