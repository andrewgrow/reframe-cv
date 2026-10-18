package reframecv.ui.compose.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.components.settings.SettingsError
import reframecv.ui.components.settings.SettingsState
import reframecv.ui.components.settings.TestSettingsComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class SettingsScreenshotTest : GoldenScreenshotTest() {
    private val connection =
        ProviderConnection(
            type = ApiProviderType.OpenRouter,
            name = "Personal",
            apiKey = "example-key",
            modelId = "example/model",
        )

    @Test
    fun emptySettingsMatchesReference() = runComposeUiTest {
        setAndCaptureGolden(this) {
            RootContent(TestRootComponent(settingsComponent = TestSettingsComponent()))
        }
    }

    @Test
    fun connectionsInLightThemeMatchReference() = runComposeUiTest {
        val component =
            TestSettingsComponent(
                SettingsState(listOf(connection.copy(id = 1, active = true)), loading = false),
            )
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) {
            RootContent(TestRootComponent(settingsComponent = component))
        }
    }

    @Test
    fun connectionEditorWithModelsMatchesReference() = runComposeUiTest {
        val component =
            TestSettingsComponent(
                editor = ProviderEditorState(
                    connection,
                    listOf(AiModel("example/model", "Example model", true)),
                ),
            )
        setAndCaptureGolden(this) { RootContent(TestRootComponent(settingsComponent = component)) }
    }

    @Test
    fun credentialErrorMatchesReference() = runComposeUiTest {
        val component =
            TestSettingsComponent(
                editor = ProviderEditorState(
                    connection.copy(modelId = ""),
                    error = SettingsError.Unauthorized,
                ),
            )
        setAndCaptureGolden(this) { RootContent(TestRootComponent(settingsComponent = component)) }
    }
}
