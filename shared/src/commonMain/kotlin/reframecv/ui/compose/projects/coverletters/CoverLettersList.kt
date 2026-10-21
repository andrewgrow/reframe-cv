package reframecv.ui.compose.projects.coverletters

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
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.coverletter.CoverLetterResumeLink
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.cover_letters_select_all
import reframecv.ui.components.projects.coverletters.CoverLetterSelectionComponent
import reframecv.ui.theme.ReframeTheme

internal const val COVER_LETTERS_LIST_TAG = "coverletters.list"

@Composable
internal fun CoverLettersList(
    letters: List<CoverLetter>,
    timeZone: TimeZone,
    onEdit: (CoverLetter) -> Unit,
    selection: CoverLetterSelectionComponent,
    links: List<CoverLetterResumeLink>,
    onResume: (Long) -> Unit,
) {
    val selected by selection.state.subscribeAsState()
    val enabled = !selected.confirming && !selected.deleting
    val toggleState = when {
        selected.selected.isEmpty() -> ToggleableState.Off
        letters.all { it.id in selected.selected } -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val table = maxWidth >= ReframeTheme.tokens.dimensions.resumeTableMinWidth
        Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium)) {
            if (table) {
                CoverLetterTableHeader(toggleState, enabled, selection::onToggleAll)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoverLetterSelectAll(toggleState, enabled, selection::onToggleAll)
                    Text(stringResource(Res.string.cover_letters_select_all))
                }
            }
            LazyColumn(
                Modifier.weight(1f).testTag(COVER_LETTERS_LIST_TAG),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                items(letters, key = { it.id }) { letter ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(
                            ReframeTheme.tokens.spacing.medium,
                        ),
                    ) {
                        CoverLetterRow(
                            letter,
                            timeZone,
                            table,
                            CoverLetterRowSelection(letter.id in selected.selected, enabled) {
                                selection.onToggle(letter.id)
                            },
                            CoverLetterRowLinks(
                                links.filter {
                                    it.letterId == letter.id
                                },
                                onResume,
                            ),
                        ) { if (enabled) onEdit(letter) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
