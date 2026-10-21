package reframecv.ui.compose.projects.coverletters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.letter_create_title
import reframecv.shared.generated.resources.letter_title
import reframecv.ui.components.projects.coverletters.CoverLetterEditorComponent
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.theme.ReframeTheme

@Composable
fun CoverLetterEditorContent(component: CoverLetterEditorComponent) {
    val state by component.state.subscribeAsState()
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .testTag("cover_letter.editor").pageBackHandler(
                component::onClose,
                enabled =
                    !state.busy && !state.confirming && !state.exportVisible,
            ),
    ) {
        Box(Modifier.padding(ReframeTheme.tokens.spacing.medium)) {
            PageHeader(
                stringResource(
                    if (state.record.id ==
                        0L
                    ) {
                        Res.string.letter_create_title
                    } else {
                        Res.string.letter_title
                    },
                ),
                component::onClose,
                enabled = !state.busy,
            )
        }
        CoverLetterFields(component, Modifier.weight(1f))
        HorizontalDivider()
        CoverLetterFooter(component)
    }
    CoverLetterDeleteDialog(component)
    if (state.exportVisible) {
        CoverLetterExportDialog(
            component.exportComponent,
            component::onCloseExport,
        )
    }
}
