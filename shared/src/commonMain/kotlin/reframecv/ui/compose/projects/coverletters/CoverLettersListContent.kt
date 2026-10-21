package reframecv.ui.compose.projects.coverletters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.SelectableText
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.cover_letters_empty
import reframecv.shared.generated.resources.dashboard_cover_letters
import reframecv.ui.components.projects.coverletters.CoverLettersComponent
import reframecv.ui.components.projects.coverletters.CoverLettersState
import reframecv.ui.compose.common.LoadingContent
import reframecv.ui.compose.common.PageHeader
import reframecv.ui.compose.common.pageBackHandler
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun CoverLettersListContent(component: CoverLettersComponent) {
    val state by component.uiState.subscribeAsState()
    val selection by component.selection.state.subscribeAsState()
    val timeZone = remember { TimeZone.currentSystemDefault() }
    key(component.projectId) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                Modifier.fillMaxSize().background(
                    MaterialTheme.colorScheme.background,
                ).safeContentPadding().pageBackHandler(
                    component::onBack,
                    enabled = !selection.confirming,
                ),
            ) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(ReframeTheme.tokens.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
                ) {
                    PageHeader(
                        stringResource(Res.string.dashboard_cover_letters),
                        component::onBack,
                        enabled = !selection.deleting,
                    )
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (val current = state) {
                            CoverLettersState.Loading -> LoadingContent()

                            CoverLettersState.LoadFailed -> CoverLettersLoadError()

                            is CoverLettersState.Ready -> if (current.letters.isEmpty()) {
                                SelectableText(
                                    stringResource(Res.string.cover_letters_empty),
                                    color = ReframeTheme.colorScheme.hint,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            } else {
                                CoverLettersList(
                                    current.letters,
                                    timeZone,
                                    component::onEdit,
                                    component.selection,
                                    current.linkedVacancies,
                                    component::onOpenVacancy,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
                CoverLettersListControls(component)
            }
            CoverLetterSelectionConfirmation(component)
        }
    }
}
