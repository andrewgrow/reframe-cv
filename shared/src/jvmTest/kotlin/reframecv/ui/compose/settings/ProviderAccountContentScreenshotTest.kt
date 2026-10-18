package reframecv.ui.compose.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.time.Instant
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderBudget
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.ProviderCreditBalance
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

    @Test
    fun failedBalanceKeepsCachedCreditsAndBudgetsMatchReference() =
        capture(ThemeMode.Dark, balanceFailed = true)

    @Test
    fun missingManagementKeyKeepsBudgetsAvailableMatchesReference() =
        capture(ThemeMode.Light, managementKey = "")

    private fun capture(
        theme: ThemeMode,
        failed: Boolean = false,
        balanceFailed: Boolean = false,
        managementKey: String = "management-key",
    ) = runDesktopComposeUiTest(ScreenSize(800.dp, 900.dp)) {
        setAndCaptureGolden(
            this,
            screenSize = ScreenSize(800.dp, 900.dp),
            themeMode = theme,
            captureTag = "settings.account.snapshot",
        ) {
            Column(
                Modifier.padding(
                    ReframeTheme.tokens.spacing.medium,
                ).testTag(
                    "settings.account.snapshot",
                ).background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.large),
            ) {
                val state = ProviderEditorState(
                    ProviderConnection(
                        type = ApiProviderType.OpenRouter,
                        name = "Work",
                        apiKey = "key",
                        modelId = "model",
                        accountInfo = info,
                        managementApiKey = managementKey,
                        creditBalance = if (managementKey.isEmpty()) {
                            null
                        } else {
                            ProviderCreditBalance(
                                5.0,
                                4.84480375,
                                info.fetchedAt,
                            )
                        },
                    ),
                    accountError = if (failed) SettingsError.AccountRefresh else null,
                    balanceError = balanceFailed,
                )
                ProviderManagementContent(state, onRefresh = {})
                ProviderAccountContent(state, onRefresh = {})
            }
        }
    }
}
