package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.vacancy_description
import reframecv.shared.generated.resources.vacancy_resize_description
import reframecv.ui.compose.common.ResizableTextField
import reframecv.ui.theme.ReframeTheme

@Composable
internal fun VacancyDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    scroll: ScrollState,
    viewport: Rect,
) {
    ResizableTextField(
        value, onValueChange, enabled, scroll, viewport,
        label = stringResource(Res.string.vacancy_description),
        resizeLabel = stringResource(Res.string.vacancy_resize_description),
        tag = "vacancy.description",
        minimumHeight = ReframeTheme.tokens.dimensions.vacancyDescriptionMinHeight,
        initialHeight = ReframeTheme.tokens.dimensions.vacancyDescriptionDefaultHeight,
    )
}
