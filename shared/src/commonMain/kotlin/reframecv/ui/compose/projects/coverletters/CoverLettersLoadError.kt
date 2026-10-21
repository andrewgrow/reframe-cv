package reframecv.ui.compose.projects.coverletters

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.cover_letters_load_error
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun CoverLettersLoadError() {
    SelectableText(
        stringResource(Res.string.cover_letters_load_error),
        color = ReframeTheme.colorScheme.critical,
        style = MaterialTheme.typography.bodyLarge,
    )
}
