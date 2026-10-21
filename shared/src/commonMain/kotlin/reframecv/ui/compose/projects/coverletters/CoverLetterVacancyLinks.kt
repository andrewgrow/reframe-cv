package reframecv.ui.compose.projects.coverletters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.coverletter.CoverLetterVacancyLink
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_close
import reframecv.shared.generated.resources.resumes_linked_vacancies
import reframecv.shared.generated.resources.resumes_more_links
import reframecv.ui.compose.common.FastScrollColumn
import reframecv.ui.compose.common.ResizableDialog
import reframecv.ui.theme.ReframeTheme

private const val PREVIEW_LINK_COUNT = 2

@Composable
internal fun CoverLetterVacancyLinks(
    links: CoverLetterRowLinks,
    enabled: Boolean,
    onMore: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        if (links.records.isEmpty()) Text("—", color = ReframeTheme.colorScheme.hint)
        links.records.take(PREVIEW_LINK_COUNT).forEach { link ->
            CoverLetterVacancyLinkText(link, enabled) { links.open(link.vacancyId) }
        }
        val remaining = links.records.size - PREVIEW_LINK_COUNT
        if (remaining > 0) {
            Text(
                stringResource(Res.string.resumes_more_links, remaining),
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.testTag(
                    "coverletters.links.more.${links.records.first().letterId}",
                )
                    .clickable(enabled = enabled, role = Role.Button, onClick = onMore),
            )
        }
    }
}

@Composable
internal fun CoverLetterVacanciesDialog(
    links: CoverLetterRowLinks,
    letterName: String,
    onClose: () -> Unit,
) {
    ResizableDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("coverletters.links.dialog"),
        title = { Text(stringResource(Res.string.resumes_linked_vacancies)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium)) {
                Text(letterName, style = ReframeTheme.tokens.typography.titleMedium)
                FastScrollColumn(
                    Modifier.fillMaxWidth().heightIn(
                        max = ReframeTheme.tokens.dimensions.documentLinksDialogHeight,
                    ).testTag("coverletters.links.list"),
                    verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
                ) {
                    links.records.forEach { link ->
                        CoverLetterVacancyLinkText(link, enabled = true) {
                            onClose()
                            links.open(link.vacancyId)
                        }
                    }
                }
            }
        },
        actions = {
            TextButton(onClose, Modifier.testTag("coverletters.links.close")) {
                Text(stringResource(Res.string.resume_close))
            }
        },
    )
}

@Composable
private fun CoverLetterVacancyLinkText(
    link: CoverLetterVacancyLink,
    enabled: Boolean,
    open: () -> Unit,
) {
    Text(
        link.name,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.testTag("coverletters.link.${link.letterId}.${link.vacancyId}")
            .clickable(enabled = enabled, role = Role.Button, onClick = open),
    )
}
