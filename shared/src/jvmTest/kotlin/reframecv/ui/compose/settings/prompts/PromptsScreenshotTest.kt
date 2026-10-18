package reframecv.ui.compose.settings.prompts

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.settings.TestSettingsComponent
import reframecv.ui.components.settings.prompts.PromptEditorError
import reframecv.ui.components.settings.prompts.PromptEditorState
import reframecv.ui.components.settings.prompts.PromptsState
import reframecv.ui.components.settings.prompts.TestPromptsComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class PromptsScreenshotTest : GoldenScreenshotTest() {
    private val prompts = listOf(
        Prompt(
            id = 1,
            title = "Vacancy extraction",
            component = PromptComponent.Vacancies,
            text = "Extract the job posting.",
        ),
        Prompt(
            id = 2,
            component = PromptComponent.Resume,
            title = "Resume extraction",
            text = "Extract all facts.",
        ),
        Prompt(
            id = 3,
            title = "General writing style",
            text = "Keep the original language.",
            active = false,
        ),
        Prompt(id = 4, component = PromptComponent.Projects, text = "Help organize projects."),
    )

    @Test
    fun emptyPromptListMatchesReference() = runDesktopComposeUiTest {
        val component = TestPromptsComponent(PromptsState(loading = false))
        setAndCaptureGolden(this) {
            RootContent(
                TestRootComponent(settingsComponent = TestSettingsComponent(prompts = component)),
            )
        }
    }

    @Test
    fun promptListInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val component = TestPromptsComponent(PromptsState(prompts, loading = false))
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) {
            RootContent(
                TestRootComponent(settingsComponent = TestSettingsComponent(prompts = component)),
            )
        }
    }

    @Test
    fun promptEditorMatchesReference() = runDesktopComposeUiTest {
        val component =
            TestPromptsComponent(PromptsState(), PromptEditorState(prompts[1], successfulSaves = 1))
        setAndCaptureGolden(this) {
            RootContent(
                TestRootComponent(settingsComponent = TestSettingsComponent(prompts = component)),
            )
        }
    }

    @Test
    fun promptValidationMatchesReference() = runDesktopComposeUiTest {
        val component =
            TestPromptsComponent(
                PromptsState(),
                PromptEditorState(Prompt(), error = PromptEditorError.Validation),
            )
        setAndCaptureGolden(this) {
            RootContent(
                TestRootComponent(settingsComponent = TestSettingsComponent(prompts = component)),
            )
        }
    }

    @Test
    fun promptSaveFailureMatchesReference() = runDesktopComposeUiTest {
        val component =
            TestPromptsComponent(
                PromptsState(),
                PromptEditorState(prompts[2], error = PromptEditorError.Save),
            )
        setAndCaptureGolden(this) {
            RootContent(
                TestRootComponent(settingsComponent = TestSettingsComponent(prompts = component)),
            )
        }
    }

    @Test
    fun promptLoadFailureMatchesReference() = runDesktopComposeUiTest {
        val component = TestPromptsComponent(PromptsState(loading = false, failed = true))
        setAndCaptureGolden(this) {
            RootContent(
                TestRootComponent(settingsComponent = TestSettingsComponent(prompts = component)),
            )
        }
    }
}
