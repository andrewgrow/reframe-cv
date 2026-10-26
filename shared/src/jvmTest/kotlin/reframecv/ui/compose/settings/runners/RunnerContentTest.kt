package reframecv.ui.compose.settings.runners

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.ComponentTest
import reframecv.ui.components.settings.DefaultSettingsComponent
import reframecv.ui.components.settings.runners.RunnerDraft
import reframecv.ui.compose.settings.SettingsContent
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.threading.runOnUiThread

@OptIn(ExperimentalTestApi::class)
class RunnerContentTest : ComponentTest() {
    @Test
    fun editsRunnerInNestedPageAndEscapeReturnsToProviderAndDeletionRequiresConfirmation() =
        runDesktopComposeUiTest(width = 800, height = 700) {
            val connection = ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Sources",
                configuration = ApifyConfiguration("token"),
            )
            val id = runBlocking { dependencies.providerSettingsUseCases.save(connection) }
            val settings = runOnUiThread {
                lifecycle.resume()
                DefaultSettingsComponent(appComponentContext()) {}.also {
                    it.onEdit(connection.copy(id = id))
                }
            }
            setContent { ReframeTheme { SettingsContent(settings) } }
            onNodeWithTag("runners.add").performScrollTo().performClick()
            onNodeWithTag("runner.editor").assertExists()
            onNodeWithTag("runner.save").performClick()
            onNodeWithTag("runner.feedback").assertExists()
            assertTrue(dependencies.connectionRunnersRepository.records.value.isEmpty())
            onNodeWithTag("runner.name").performScrollTo().performTextReplacement("Android remote")
            onNodeWithTag("runner.urls").performScrollTo().performTextReplacement(
                "https://www.linkedin.com/jobs/search/?keywords=kotlin",
            )
            onNodeWithTag("runner.limit").performScrollTo().performTextReplacement("25")
            onNodeWithTag("runner.save").performClick()
            waitForIdle()
            onNodeWithTag("runner.delete").assertExists()
            assertEquals(1, dependencies.connectionRunnersRepository.records.value.size)
            onNodeWithTag("runner.runs").performClick()
            onNodeWithTag("runner.runs.page").assertExists()
            onNodeWithTag("runner.run.start").performClick()
            waitUntil(timeoutMillis = 5000) {
                dependencies.runnerExecutionRepository.resultReads ==
                    1
            }
            waitForIdle()
            onNodeWithTag("runner.run.refresh").assertExists()
            assertEquals(1, dependencies.runnerExecutionRepository.starts)
            onRoot().performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("runner.run.1").assertExists()
            onRoot().performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("runner.editor").assertExists()
            onRoot().performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("settings.editor").assertExists()
            onNodeWithTag(
                "runners.row.1",
            ).performScrollTo().assertTextContains("Android remote").performClick()
            onNodeWithTag("runner.delete").performClick()
            onNodeWithTag("runner.delete.dialog").assertExists()
            onNodeWithTag("confirmation.input").assertDoesNotExist()
            onNodeWithTag("runner.delete.dialog").performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("runner.delete.dialog").assertDoesNotExist()
            onNodeWithTag("runner.editor").assertExists()
            onNodeWithTag("runner.close").performClick()
            onNodeWithTag("runners.row.1").performScrollTo().assertExists()
        }

    @Test
    fun settingsShortcutReturnsFromRunnerSettingsToRunnerHistory() =
        runDesktopComposeUiTest(width = 800, height = 700) {
            val connection = ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Example source",
                configuration = ApifyConfiguration("fictional-token"),
            )
            val id = runBlocking { dependencies.providerSettingsUseCases.save(connection) }
            val runnerId = runBlocking {
                dependencies.connectionRunnersUseCases.save(
                    RunnerDraft(
                        name = "Target search",
                        searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin",
                    )
                        .applyTo(RunnerDraft.newRunner(id)),
                )
            }
            val settings = runOnUiThread {
                lifecycle.resume()
                DefaultSettingsComponent(appComponentContext()) {}.also {
                    it.onEdit(connection.copy(id = id))
                }
            }
            setContent { ReframeTheme { SettingsContent(settings) } }
            onNodeWithTag("runners.row.$runnerId").performScrollTo().performClick()
            onNodeWithTag("runner.runs").performClick()
            onNodeWithTag("runner.run.settings").performClick()
            waitForIdle()
            onNodeWithTag("runner.editor").assertExists()
            onNodeWithTag("runner.name").assertTextContains("Target search")
            onNodeWithTag("settings.editor").assertDoesNotExist()
            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            onNodeWithTag("runner.runs.page").assertExists()
            assertEquals(0, dependencies.runnerExecutionRepository.starts)
        }
}
