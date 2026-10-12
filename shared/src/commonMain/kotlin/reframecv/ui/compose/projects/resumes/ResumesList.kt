package reframecv.ui.compose.projects.resumes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.datetime.TimeZone
import reframecv.domain.models.resume.Resume
import reframecv.ui.theme.ReframeTheme

internal const val RESUMES_LIST_TAG = "resumes.list"

@Composable
internal fun ResumesList(resumes: List<Resume>, timeZone: TimeZone) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val table = maxWidth >= ReframeTheme.tokens.dimensions.resumeTableMinWidth
        Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium)) {
            if (table) ResumeTableHeader()
            LazyColumn(
                Modifier.weight(1f).testTag(RESUMES_LIST_TAG),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                items(resumes, key = { it.id }) { resume ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(
                            ReframeTheme.tokens.spacing.medium,
                        ),
                    ) {
                        ResumeRow(resume, timeZone, table)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
