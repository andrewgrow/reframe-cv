package reframecv.ui.compose.projects.coverletters

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.coverletters.CoverLetterImportState
import reframecv.ui.components.projects.coverletters.TestCoverLetterEditorComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class CoverLetterImportScreenshotTest : GoldenScreenshotTest() {
    @Test fun creationMatchesReference() = capture(CoverLetterImportState.Idle)

    @Test fun importingMatchesReference() = capture(CoverLetterImportState.Importing)

    @Test fun importedLetterMatchesReference() = capture(CoverLetterImportState.Imported)

    @Test fun failedImportInLightThemeMatchesReference() =
        capture(CoverLetterImportState.Failed, ThemeMode.Light)
    private fun capture(state: CoverLetterImportState, theme: ThemeMode = ThemeMode.Dark) =
        runDesktopComposeUiTest {
            val imported = state == CoverLetterImportState.Imported
            val component = TestCoverLetterEditorComponent(
                CoverLetter(
                    projectId = 7,
                    name = if (imported) "Mobile developer" else "",
                    createdAt = 0,
                    updatedAt = 0,
                    content = if (imported) {
                        (
                            "Dear team,\n\nI am applying for the Mobile Developer role." +
                                "\n\nKind regards,\nAlex"
                            )
                    } else {
                        ""
                    },
                ),
            )
            component.importComponent.state.value = state
            component.state.value =
                component.state.value.copy(busy = state == CoverLetterImportState.Importing)
            setAndCaptureGolden(this, themeMode = theme) { CoverLetterEditorContent(component) }
        }
}
