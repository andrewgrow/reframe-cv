package reframecv.ui.compose.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import kotlin.time.Instant
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderBudget
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.settings.ProviderEditorState
import reframecv.ui.components.settings.SettingsError
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ProviderAccountContentScreenshotTest : GoldenScreenshotTest() {
    private val workspace = ProviderBudget(
        "workspace",
        1.0,
        0.125,
        0.875,
        Instant.parse("2026-10-19T00:00:00Z").toEpochMilliseconds(),
        "daily",
    )
    private val info = ProviderAccountInfo(
        listOf(workspace, ProviderBudget("key_limit", 50.0, 0.125, 49.875)),
        workspace,
        Instant.parse("2026-10-25T09:38:00Z").toEpochMilliseconds(),
        Instant.parse("2026-10-18T09:39:58Z").toEpochMilliseconds(),
    )

    @Test
    fun effectiveBudgetAndKeyLimitMatchReference() = capture(ThemeMode.Dark)

    @Test
    fun failedRefreshKeepsCachedBudgetsInLightTheme() = capture(ThemeMode.Light, failed = true)

    private fun capture(theme: ThemeMode, failed: Boolean = false) = runDesktopComposeUiTest {
        setAndCaptureGolden(this, themeMode = theme, captureTag = "settings.account") {
            Box(Modifier.padding(ReframeTheme.tokens.spacing.medium)) {
                ProviderAccountContent(
                    ProviderEditorState(
                        ProviderConnection(
                            type = ApiProviderType.OpenRouter,
                            name = "Work",
                            apiKey = "key",
                            modelId = "model",
                            accountInfo = info,
                        ),
                        accountError = if (failed) SettingsError.AccountRefresh else null,
                    ),
                    onRefresh = {},
                )
            }
        }
    }
}
