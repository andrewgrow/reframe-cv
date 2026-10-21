package reframecv.ui.compose.projects.coverletters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.coverletter.CoverLetterVacancyLink
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.cover_letter_name
import reframecv.shared.generated.resources.cover_letters_added
import reframecv.shared.generated.resources.cover_letters_linked
import reframecv.shared.generated.resources.cover_letters_select
import reframecv.shared.generated.resources.cover_letters_select_all
import reframecv.ui.compose.common.documentDate
import reframecv.ui.theme.ReframeTheme

/** Separates checkbox interaction from opening a letter. */
internal data class CoverLetterRowSelection(
    val checked: Boolean,
    val enabled: Boolean,
    val toggle: () -> Unit,
)

/** Supplies linked-vacancy references and their navigation callback. */
internal data class CoverLetterRowLinks(
    val records: List<CoverLetterVacancyLink>,
    val open: (Long) -> Unit,
)

@Composable
internal fun CoverLetterRow(
    letter: CoverLetter,
    timeZone: TimeZone,
    table: Boolean,
    selection: CoverLetterRowSelection,
    links: CoverLetterRowLinks,
    onEdit: () -> Unit,
) {
    var showLinks by remember(letter.id) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().testTag("coverletters.row.${letter.id}"),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverLetterRowCheckbox(letter, selection)
        if (table) {
            SelectableText(
                letter.name,
                modifier = letterTextClick(onEdit),
                selectionModifier = Modifier.weight(2f),
                style = MaterialTheme.typography.bodyLarge,
            )
            CoverLetterVacancyLinks(links, selection.enabled, {
                showLinks = true
            }, Modifier.weight(1f))
            SelectableText(
                documentDate(letter.createdAt, timeZone),
                modifier = letterTextClick(onEdit),
                selectionModifier = Modifier.width(
                    ReframeTheme.tokens.dimensions.documentDateWidth,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
            ) {
                SelectableText(
                    letter.name,
                    modifier = letterTextClick(onEdit),
                    style = MaterialTheme.typography.bodyLarge,
                )
                CoverLetterVacancyLinks(links, selection.enabled, {
                    showLinks = true
                }, Modifier.fillMaxWidth())
                SelectableText(
                    documentDate(letter.createdAt, timeZone),
                    modifier = letterTextClick(onEdit),
                    color = ReframeTheme.colorScheme.hint,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (showLinks) CoverLetterVacanciesDialog(links, letter.name) { showLinks = false }
}

@Composable
internal fun CoverLetterTableHeader(state: ToggleableState, enabled: Boolean, toggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverLetterSelectAll(state, enabled, toggle)
        SelectableText(
            stringResource(Res.string.cover_letter_name),
            selectionModifier = Modifier.weight(2f),
            style = MaterialTheme.typography.titleMedium,
        )
        SelectableText(
            stringResource(Res.string.cover_letters_linked),
            selectionModifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        SelectableText(
            stringResource(Res.string.cover_letters_added),
            selectionModifier = Modifier.width(ReframeTheme.tokens.dimensions.documentDateWidth),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
internal fun CoverLetterSelectAll(state: ToggleableState, enabled: Boolean, toggle: () -> Unit) {
    val label = stringResource(Res.string.cover_letters_select_all)
    TriStateCheckbox(
        state,
        toggle,
        enabled = enabled,
        modifier = Modifier.size(
            ReframeTheme.tokens.dimensions.iconButtonSize,
        ).testTag("coverletters.select.all").semantics {
            contentDescription =
                label
        },
    )
}

@Composable
private fun letterTextClick(onEdit: () -> Unit): Modifier = Modifier.clickable(
    interactionSource = remember {
        MutableInteractionSource()
    },
    indication = null,
    role = Role.Button,
    onClick = onEdit,
)

@Composable
private fun CoverLetterRowCheckbox(letter: CoverLetter, selection: CoverLetterRowSelection) {
    val label = stringResource(Res.string.cover_letters_select, letter.name)
    Checkbox(
        selection.checked,
        { selection.toggle() },
        enabled = selection.enabled,
        modifier = Modifier.size(
            ReframeTheme.tokens.dimensions.iconButtonSize,
        ).testTag("coverletters.select.${letter.id}").semantics {
            contentDescription =
                label
        },
    )
}
