package reframecv.ui.compose.settings.runners

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.settings.runners.RunnerCopyState
import reframecv.ui.components.settings.runners.RunnerDraft
import reframecv.ui.components.settings.runners.RunnerEditorState
import reframecv.ui.components.settings.runners.TestRunnerEditorComponent
import reframecv.usecase.SavedRunner

@OptIn(ExperimentalTestApi::class)
class RunnerCopyDialogTest : GoldenScreenshotTest() {
    private fun editor(): TestRunnerEditorComponent {
        val component = TestRunnerEditorComponent(RunnerEditorState(RunnerDraft.newRunner(1)))
        component.copy.state.value = RunnerCopyState(
            sources = listOf(
                SavedRunner(
                    RunnerDraft(
                        name = "Example search",
                        searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin",
                        maxResults = "25",
                    )
                        .applyTo(RunnerDraft.newRunner(2)).copy(id = 5),
                    ProviderConnection(
                        id = 2,
                        type = ApiProviderType.Apify,
                        name = "Example connection",
                        configuration = ApifyConfiguration("fictional-token"),
                    ),
                ),
            ),
        )
        return component
    }

    @Test
    fun sourcePickerMatchesReference() = runDesktopComposeUiTest {
        val editor = editor()
        setGoldenContent(this) { RunnerEditorContent(editor) }
        onNodeWithTag("runner.copy").performClick()
        captureGolden(this, "runner.copy.dialog")
    }

    @Test
    fun editableCopyMatchesReferenceAndAppliesChanges() = runDesktopComposeUiTest {
        val editor = editor()
        setGoldenContent(this) { RunnerEditorContent(editor) }
        onNodeWithTag("runner.copy").performClick()
        onNodeWithTag("runner.copy.source.5").performClick()
        captureGolden(this, "runner.copy.dialog")
        onNodeWithTag("runner.copy.name").performTextReplacement("Edited search")
        onNodeWithTag("runner.copy.limit").performScrollTo().performTextReplacement("50")
        onNodeWithTag("runner.copy.apply").performClick()
        onNodeWithTag("runner.copy.dialog").assertDoesNotExist()
        assertEquals("Edited search", editor.state.value.draft.name)
        assertEquals("50", editor.state.value.draft.maxResults)
        assertEquals(0L, editor.state.value.runner.id)
    }

    @Test
    fun copyingAnotherOperationShowsItsFieldsAndUpdatesTheEditorType() = runDesktopComposeUiTest {
        val editor = editor()
        val original = editor.copy.state.value.sources.single()
        val ams = RunnerDraft(
            operation = ConnectionOperation.AmsJobSearch,
            name = "Regional search",
            query = "Kotlin",
            location = "Wien",
        )
            .applyTo(RunnerDraft.newRunner(2)).copy(id = 6)
        editor.copy.state.value = editor.copy.state.value.copy(
            sources = listOf(original, original.copy(runner = ams)),
        )
        setGoldenContent(this) { RunnerEditorContent(editor) }
        onNodeWithTag("runner.copy").performClick()
        onNodeWithTag("runner.copy.source.6").performScrollTo().performClick()
        onNodeWithTag("runner.copy.query").performScrollTo()
            .performTextReplacement("Android")
        onNodeWithTag("runner.copy.apply").performClick()
        assertEquals(ConnectionOperation.AmsJobSearch, editor.state.value.draft.operation)
        assertEquals("Android", editor.state.value.draft.query)
        onNodeWithTag("runner.query").assertExists()
    }

    @Test
    fun cancelDiscardsCopyEditsAndSavedRunnerHasNoCopyButton() = runDesktopComposeUiTest {
        val editor = editor()
        setGoldenContent(this) { RunnerEditorContent(editor) }
        onNodeWithTag("runner.copy").performClick()
        onNodeWithTag("runner.copy.source.5").performClick()
        onNodeWithTag("runner.copy.name").performTextReplacement("Discard me")
        onNodeWithTag("runner.copy.cancel").performClick()
        assertEquals("", editor.state.value.draft.name)
        editor.state.value =
            editor.state.value.copy(runner = editor.state.value.runner.copy(id = 10))
        onNodeWithTag("runner.copy").assertDoesNotExist()
    }
}
