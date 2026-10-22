package reframecv.ui.compose.settings

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.components.settings.TestProviderEditorComponent

@OptIn(ExperimentalTestApi::class)
class ApifySettingsScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun apifyConnectionShowsTokenWithoutAiOrManagementFields() = runDesktopComposeUiTest {
        setAndCaptureGolden(this) {
            ProviderEditorContent(
                TestProviderEditorComponent(
                    ProviderEditorState(
                        ProviderConnection(
                            type = ApiProviderType.Apify,
                            name = "Job sources",
                            configuration = ApifyConfiguration("example-token"),
                        ),
                    ),
                ),
            )
        }
    }
}
