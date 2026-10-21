package reframecv.ui.compose.projects.resumes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.state.ToggleableState
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.ResumeVacancyLink
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resumes_select_all
import reframecv.ui.components.projects.resumes.ResumeSelectionComponent
import reframecv.ui.theme.ReframeTheme

internal const val RESUMES_LIST_TAG = "resumes.list"

@Composable
internal fun ResumesList(
    resumes: List<Resume>,
    timeZone: TimeZone,
    onEdit: (Resume) -> Unit,
    selection: ResumeSelectionComponent,
    links: List<ResumeVacancyLink>,
    onVacancy: (Long) -> Unit,
) {
    val selected by selection.state.subscribeAsState()
    val enabled = !selected.confirming && !selected.deleting
    val toggleState = when {
        selected.selected.isEmpty() -> ToggleableState.Off
        resumes.all { it.id in selected.selected } -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val table = maxWidth >= ReframeTheme.tokens.dimensions.resumeTableMinWidth
        Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium)) {
            if (table) {
                ResumeTableHeader(toggleState, enabled, selection::onToggleAll)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ResumeSelectAll(toggleState, enabled, selection::onToggleAll)
                    Text(stringResource(Res.string.resumes_select_all))
                }
            }
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
                        ResumeRow(
                            resume,
                            timeZone,
                            table,
                            ResumeRowSelection(resume.id in selected.selected, enabled) {
                                selection.onToggle(resume.id)
                            },
                            ResumeRowLinks(
                                links.filter {
                                    it.resumeId == resume.id
                                },
                                onVacancy,
                            ),
                        ) { if (enabled) onEdit(resume) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
