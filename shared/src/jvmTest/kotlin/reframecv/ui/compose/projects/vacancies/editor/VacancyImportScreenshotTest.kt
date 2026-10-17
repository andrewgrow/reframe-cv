package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancyImportState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancyImportScreenshotTest : GoldenScreenshotTest() {
    @Test fun importingMatchesReference() = capture(VacancyImportState.Importing)

    @Test fun importedFieldsMatchReference() = capture(VacancyImportState.Imported)

    @Test fun failedImportInLightThemeMatchesReference() =
        capture(VacancyImportState.Failed, ThemeMode.Light)

    private fun capture(state: VacancyImportState, theme: ThemeMode = ThemeMode.Dark) =
        runDesktopComposeUiTest {
            val component = TestVacancyEditorComponent()
            component.draft.value = VacancyDraft(
                "Kotlin Developer",
                "Example",
                "Build applications.",
                "https://example.com/job",
                "Kotlin, Compose",
            )
            component.importComponent.state.value = state
            setGoldenContent(this, themeMode = theme) { VacancyEditorContent(component) }
            captureGolden(this, VACANCY_EDITOR_TAG)
        }
}
