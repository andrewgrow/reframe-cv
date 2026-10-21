package reframecv.ui.compose.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.settings.SettingsImportReport
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.settings.SettingsTransferOperation
import reframecv.ui.components.settings.SettingsTransferState
import reframecv.ui.components.settings.TestSettingsTransferComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class SettingsTransferScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun defaultExportExcludesKeysAndFileActionsRemainAccessible() = runComposeUiTest {
        val component = TestSettingsTransferComponent()
        setGoldenContent(this) { SettingsTransferContent(component) }
        onNodeWithTag("settings.transfer.keys").assertIsOff()
        onNodeWithTag("settings.transfer.warning").assertDoesNotExist()
        captureGolden(this)
        onNodeWithTag("settings.transfer.keys").performClick().assertIsOn()
        onNodeWithTag("settings.transfer.warning").assertExists()
        onNodeWithTag("settings.transfer.import").performClick()
        onNodeWithTag("settings.transfer.export").performClick()
        assertEquals(1, component.imports)
        assertEquals(1, component.exports)
    }

    @Test
    fun explicitKeyConsentShowsPlainTextWarning() = runComposeUiTest {
        val component = TestSettingsTransferComponent(SettingsTransferState(includeApiKeys = true))
        setAndCaptureGolden(this) { SettingsTransferContent(component) }
    }

    @Test
    fun restoredSettingsReportInLightTheme() = runComposeUiTest {
        val component = TestSettingsTransferComponent(
            SettingsTransferState(
                report = SettingsImportReport(
                    2,
                    5,
                    duplicates = 1,
                    skipped = 2,
                    windowRestored = true,
                ),
            ),
        )
        setAndCaptureGolden(this, themeMode = ThemeMode.Light) {
            SettingsTransferContent(component)
        }
    }

    @Test
    fun invalidImportRemainsRetryable() = runComposeUiTest {
        val component =
            TestSettingsTransferComponent(
                SettingsTransferState(error = SettingsTransferOperation.Import),
            )
        setAndCaptureGolden(this) { SettingsTransferContent(component) }
    }
}
