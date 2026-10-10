package reframecv.ui.compose.projects.vacancies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.navigation_back
import reframecv.shared.generated.resources.navigation_retry
import reframecv.shared.generated.resources.vacancy_add
import reframecv.ui.compose.common.FillButton
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacanciesControls(onBack: () -> Unit, onRetry: (() -> Unit)? = null) {
    val tokens = ReframeTheme.tokens
    FlowRow(
        Modifier.fillMaxWidth().padding(tokens.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(
            tokens.spacing.medium,
            Alignment.CenterHorizontally,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        FillButton(
            text = stringResource(Res.string.vacancy_add),
            // Creation is deliberately deferred while the vacancy list is being refined.
            onClick = {},
            outlineState = FillButton.OutlineState.Visible,
        )
        onRetry?.let { retry ->
            FillButton(
                text = stringResource(Res.string.navigation_retry),
                onClick = retry,
                outlineState = FillButton.OutlineState.Visible,
            )
        }
        FillButton(
            text = stringResource(Res.string.navigation_back),
            onClick = onBack,
            outlineState = FillButton.OutlineState.Visible,
        )
    }
}
