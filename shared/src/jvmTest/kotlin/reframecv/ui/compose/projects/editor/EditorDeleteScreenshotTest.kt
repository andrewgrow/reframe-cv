package reframecv.ui.compose.projects.editor

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import kotlin.test.Test
import org.jetbrains.compose.resources.getString
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.action_cancel
import reframecv.shared.generated.resources.action_delete
import reframecv.shared.generated.resources.action_full_delete
import reframecv.shared.generated.resources.action_update
import reframecv.shared.generated.resources.project_delete_confirmation
import reframecv.shared.generated.resources.project_name
import reframecv.testing.GoldenScreenshotTest
import reframecv.testing.getTestString
import reframecv.ui.components.projects.editor.EditorDeleteState
import reframecv.ui.components.projects.editor.TestEditorComponent
import reframecv.ui.theme.ReframeTheme.ThemeMode

private const val PROJECT_NAME = "Test Project"

@OptIn(ExperimentalTestApi::class)
class EditorDeleteScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun emptyDeletionFieldsWithCancelFocusedMatchesReference() = runDesktopComposeUiTest {
        val projectNameLabel = getString(Res.string.project_name)
        val confirmationLabel = getString(Res.string.project_delete_confirmation)
        val cancelLabel = getString(Res.string.action_cancel)
        setGoldenContent(this) { EditorContent(TestEditorComponent(initialName = PROJECT_NAME)) }

        onNodeWithText(projectNameLabel).performTextReplacement("")
        onNodeWithText(getString(Res.string.action_delete)).performClick()
        onNodeWithText(confirmationLabel).assertIsFocused()
        onNodeWithText(cancelLabel).performSemanticsAction(SemanticsActions.RequestFocus) { it() }

        onNodeWithText(cancelLabel).assertIsFocused()
        onNodeWithText(projectNameLabel).assertIsNotFocused().assertIsDisplayed()
        onNodeWithText(confirmationLabel).assertIsNotFocused().assertIsDisplayed()
        onNodeWithText(getString(Res.string.action_full_delete)).assertIsNotEnabled()
        onNodeWithText(getString(Res.string.action_update)).assertDoesNotExist()
        captureGolden(this, captureTag = PROJECT_EDITOR_TAG)
    }

    @Test
    fun deleteConfirmationMatchesReference() = runDesktopComposeUiTest {
        captureDeletionGolden(this)
    }

    @Test
    fun confirmedDeletionMatchesReference() = runDesktopComposeUiTest {
        captureDeletionGolden(this, confirmation = "DELETE")
    }

    @Test
    fun failedDeletionInLightThemeMatchesReference() = runDesktopComposeUiTest {
        captureDeletionGolden(
            this,
            ThemeMode.Light,
            "DELETE",
            EditorDeleteState.Failed,
        )
    }

    private fun captureDeletionGolden(
        composeUiTest: ComposeUiTest,
        themeMode: ThemeMode = ThemeMode.Dark,
        confirmation: String = "",
        deleteState: EditorDeleteState = EditorDeleteState.Idle,
    ) {
        val component = TestEditorComponent(initialName = "Android Developer")
        component.deleteState.value = deleteState
        setGoldenContent(composeUiTest, themeMode = themeMode) { EditorContent(component) }
        composeUiTest.onNodeWithText(getTestString(Res.string.action_delete)).performClick()
        composeUiTest.onNodeWithText(getTestString(Res.string.project_delete_confirmation))
            .performTextReplacement(confirmation)
        captureGolden(composeUiTest, PROJECT_EDITOR_TAG)
    }
}
