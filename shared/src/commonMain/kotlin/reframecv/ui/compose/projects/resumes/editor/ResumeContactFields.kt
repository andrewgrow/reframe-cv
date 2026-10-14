package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import reframecv.domain.models.resume.content.ResumeContact
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_contact_label
import reframecv.shared.generated.resources.resume_contact_type
import reframecv.shared.generated.resources.resume_contact_type_email
import reframecv.shared.generated.resources.resume_contact_type_github
import reframecv.shared.generated.resources.resume_contact_type_linkedin
import reframecv.shared.generated.resources.resume_contact_type_other
import reframecv.shared.generated.resources.resume_contact_type_phone
import reframecv.shared.generated.resources.resume_contact_type_unspecified
import reframecv.shared.generated.resources.resume_contact_type_website
import reframecv.shared.generated.resources.resume_contact_value
import reframecv.shared.generated.resources.resume_remove_contact
import reframecv.shared.generated.resources.resume_remove_contact_question
import reframecv.shared.generated.resources.resume_remove_contact_title
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun ResumeFormScope.ContactFields(
    entries: List<ResumeContact>,
    onChange: (List<ResumeContact>) -> Unit,
) {
    Entries(
        entries,
        onChange,
        ::ResumeContact,
        removeAction = { entry, remove ->
            ConfirmedRemoveAction(
                ConfirmationDialogContent(
                    title = Res.string.resume_remove_contact_title,
                    message = Res.string.resume_remove_contact_question,
                    confirmLabel = Res.string.resume_remove_contact,
                    cancelLabel = Res.string.resume_cancel,
                    subject = entry.label.ifBlank { entry.value }.takeIf { it.isNotBlank() },
                    tone = ConfirmationDialogTone.Critical,
                ),
                remove,
            )
        },
    ) { entry, change ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.small),
        ) {
            Box(Modifier.weight(1f)) {
                ContactTypeField(entry.type) { change(entry.copy(type = it)) }
            }
            Box(Modifier.weight(1f)) {
                Field(entry.label, Res.string.resume_contact_label, {
                    change(entry.copy(label = it))
                }, tag = "resume.contact.label")
            }
        }
        Field(entry.value, Res.string.resume_contact_value, {
            change(entry.copy(value = it))
        }, tag = "resume.contact.value")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResumeFormScope.ContactTypeField(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = contactTypeOptions.map { (type, label) -> type to stringResource(label) }
        .let { known -> if (known.any { it.first == value }) known else known + (value to value) }
    ExposedDropdownMenuBox(expanded && enabled, { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = options.first { it.first == value }.second,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(Res.string.resume_contact_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded && enabled) },
            modifier = Modifier.fillMaxWidth().testTag("resume.contact.type")
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded && enabled, { expanded = false }) {
            options.forEach { (type, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        if (enabled) onChange(type)
                    },
                    modifier = Modifier.testTag("resume.contact.type.option.$type"),
                )
            }
        }
    }
}

private val contactTypeOptions = listOf(
    "" to Res.string.resume_contact_type_unspecified,
    "email" to Res.string.resume_contact_type_email,
    "phone" to Res.string.resume_contact_type_phone,
    "linkedin" to Res.string.resume_contact_type_linkedin,
    "website" to Res.string.resume_contact_type_website,
    "github" to Res.string.resume_contact_type_github,
    "other" to Res.string.resume_contact_type_other,
)
