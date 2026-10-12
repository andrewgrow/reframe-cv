package reframecv.ui.compose.projects.resumes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.resume.Resume
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_added
import reframecv.shared.generated.resources.resume_name
import reframecv.ui.compose.common.documentDate
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeRow(resume: Resume, timeZone: TimeZone, table: Boolean) {
    val date = documentDate(resume.createdAt, timeZone)
    if (table) {
        Row(
            Modifier.fillMaxWidth().testTag("resumes.row.${resume.id}"),
            horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
        ) {
            SelectableText(
                resume.name,
                selectionModifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            SelectableText(
                date,
                selectionModifier = Modifier.width(
                    ReframeTheme.tokens.dimensions.documentDateWidth,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        Column(
            Modifier.testTag("resumes.row.${resume.id}"),
            verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        ) {
            SelectableText(resume.name, style = MaterialTheme.typography.bodyLarge)
            SelectableText(
                date,
                color = ReframeTheme.colorScheme.hint,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun ResumeTableHeader() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
    ) {
        SelectableText(
            stringResource(Res.string.resume_name),
            selectionModifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        SelectableText(
            stringResource(Res.string.resume_added),
            selectionModifier = Modifier.width(ReframeTheme.tokens.dimensions.documentDateWidth),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
