package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import reframecv.domain.models.resume.content.ResumeLink
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_add_link
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_link_label
import reframecv.shared.generated.resources.resume_link_url
import reframecv.shared.generated.resources.resume_remove_link
import reframecv.shared.generated.resources.resume_remove_link_question
import reframecv.shared.generated.resources.resume_remove_link_title
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.LinkFields(
    links: List<ResumeLink>,
    heading: StringResource,
    onChange: (List<ResumeLink>) -> Unit,
) {
    Column(
        Modifier.padding(top = ReframeTheme.tokens.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
    ) {
        Heading(heading)
        Entries(
            links,
            onChange,
            ::ResumeLink,
            Res.string.resume_add_link,
            removeAction = { link, remove ->
                ConfirmedRemoveAction(
                    ConfirmationDialogContent(
                        title = Res.string.resume_remove_link_title,
                        message = Res.string.resume_remove_link_question,
                        confirmLabel = Res.string.resume_remove_link,
                        cancelLabel = Res.string.resume_cancel,
                        subject = link.label.ifBlank { link.url }.takeIf { it.isNotBlank() },
                        tone = ConfirmationDialogTone.Critical,
                    ),
                    remove,
                )
            },
        ) { link, change ->
            Field(link.label, Res.string.resume_link_label, {
                change(link.copy(label = it))
            }, tag = "resume.link.label")
            Field(link.url, Res.string.resume_link_url, {
                change(link.copy(url = it))
            }, tag = "resume.link.url")
        }
    }
}
