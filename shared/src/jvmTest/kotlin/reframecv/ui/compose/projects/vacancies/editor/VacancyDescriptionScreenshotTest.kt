package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent

@OptIn(ExperimentalTestApi::class)
class VacancyDescriptionScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun expandedDescriptionMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) { VacancyEditorContent(TestVacancyEditorComponent()) }
        onNodeWithTag("vacancy.description").performTextReplacement(
            "Build a Kotlin application.\nWork with the team on architecture and delivery.",
        )
        onNodeWithTag("vacancy.description.resize").performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(0f, 20f))
            moveBy(Offset(0f, 100f))
            release()
        }
        captureGolden(this, VACANCY_EDITOR_TAG)
    }
}
