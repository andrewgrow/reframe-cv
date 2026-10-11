package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent

@OptIn(ExperimentalTestApi::class)
class VacancyDialogResizeScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun expandedDialogMatchesReference() = runDesktopComposeUiTest(ScreenSize(1200.dp, 1000.dp)) {
        setGoldenContent(this, ScreenSize(1200.dp, 1000.dp)) {
            VacancyEditorContent(TestVacancyEditorComponent())
        }
        onNodeWithTag("vacancy.title").performTextReplacement("Senior Android Developer")
        onNodeWithTag("vacancy.dialog.resize").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(80f, 60f))
            release()
        }
        captureGolden(this, VACANCY_EDITOR_TAG)
    }
}
