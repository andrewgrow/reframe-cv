package reframecv.ui.compose.settings.runners

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.settings.runners.ProviderRunnersState
import reframecv.ui.components.settings.runners.RunnerDraft
import reframecv.ui.components.settings.runners.RunnerEditorError
import reframecv.ui.components.settings.runners.RunnerEditorState
import reframecv.ui.components.settings.runners.TestProviderRunnersComponent
import reframecv.ui.components.settings.runners.TestRunnerEditorComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class RunnerScreenshotTest : GoldenScreenshotTest() {
    private val runner = RunnerDraft(
        name = "Kotlin remote",
        searchUrls =
            listOf(
                "https://www.linkedin.com/jobs/search/?keywords=kotlin",
                "https://de.linkedin.com/jobs/search/?keywords=android",
            ).joinToString("\n"),
    )
        .applyTo(RunnerDraft.newRunner(1)).copy(id = 1)

    @Test
    fun newRunnerMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) {
            RunnerEditorContent(
                TestRunnerEditorComponent(RunnerEditorState(RunnerDraft.newRunner(1))),
            )
        }
    }

    @Test
    fun savedRunnerInLightThemeMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, themeMode = ReframeTheme.ThemeMode.Light) {
            RunnerEditorContent(TestRunnerEditorComponent(RunnerEditorState(runner)))
        }
    }

    @Test
    fun invalidRunnerAtNarrowWidthMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, screenSize = ScreenSize(320.dp, 700.dp)) {
            RunnerEditorContent(
                TestRunnerEditorComponent(
                    RunnerEditorState(
                        RunnerDraft.newRunner(1),
                        draft = RunnerDraft(searchUrls = "Not a URL", maxResults = "0"),
                        error = RunnerEditorError.Validation,
                    ),
                ),
            )
        }
    }

    @Test
    fun deleteConfirmationMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, captureTag = "runner.delete.dialog") {
            RunnerEditorContent(
                TestRunnerEditorComponent(RunnerEditorState(runner, confirming = true)),
            )
        }
    }

    @Test
    fun runnerListWithActiveDisabledAndUnnamedRecordsMatchesReference() = runDesktopComposeUiTest {
        setAndCaptureGolden(this, screenSize = ScreenSize(600.dp, 300.dp)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Column(
                    Modifier.padding(ReframeTheme.tokens.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
                ) {
                    ProviderRunnersList(
                        TestProviderRunnersComponent(
                            ProviderRunnersState(
                                listOf(runner, runner.copy(id = 2, name = "", active = false)),
                                loading = false,
                            ),
                        ),
                        enabled = true,
                    )
                }
            }
        }
    }

    @Test
    fun jobLimitAndValidationHintsRemainVisibleWhenScrolledDown() = runDesktopComposeUiTest {
        setGoldenContent(this) {
            RunnerEditorContent(
                TestRunnerEditorComponent(
                    RunnerEditorState(
                        runner,
                        draft = RunnerDraft(searchUrls = "Not a URL", maxResults = "0"),
                        error = RunnerEditorError.Validation,
                    ),
                ),
            )
        }
        onNodeWithTag("runner.limit").performScrollTo()
        captureGolden(this)
    }
}
