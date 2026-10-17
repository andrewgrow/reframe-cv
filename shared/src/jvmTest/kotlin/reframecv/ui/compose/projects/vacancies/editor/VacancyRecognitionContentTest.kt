package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.attachments.AttachmentStatus
import reframecv.attachments.SelectedAttachment
import reframecv.domain.models.vacancyattachment.VacancyAttachment
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyRecognitionState
import reframecv.ui.components.projects.vacancies.editor.VacancySnapshotState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyRecognitionContentTest {
    @Test
    fun parsesSelectedPdfWithoutANameOrSavedVacancyAndShowsUnconfiguredFailure() =
        runComposeUiTest {
            val component = TestVacancyEditorComponent()
            setContent { ReframeTheme { VacancyEditorContent(component) } }
            onNodeWithText("Parse PDF").assertDoesNotExist()
            runOnIdle { component.snapshot.state.value = selected("source.txt") }
            onNodeWithText("Parse PDF").assertDoesNotExist()
            runOnIdle { component.snapshot.state.value = selected("job.PDF") }
            onNodeWithText("Save").assertIsNotEnabled()
            onNodeWithText("Parse PDF").performScrollTo().assertIsEnabled().performClick()
            onNodeWithText("PDF parsing is not configured yet.").performScrollTo().assertExists()
        }

    @Test
    fun processingLocksTheFormAndEscapeCancelsWithoutClosingTheEditor() = runComposeUiTest {
        var closes = 0
        val component = TestVacancyEditorComponent(close = { closes++ })
        component.snapshot.state.value = selected("job.pdf")
        component.recognition.state.value = VacancyRecognitionState.Processing
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithText("Cancel parsing").performScrollTo().assertIsEnabled()
        onNodeWithTag("vacancy.parse.progress").assertExists()
        onNodeWithText("Attach source file").assertIsNotEnabled()
        onNodeWithText("Remove").assertIsNotEnabled()
        onNodeWithTag("vacancy.name").assertIsNotEnabled()
        onNodeWithText("Import").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onRoot().performKeyInput { pressKey(Key.Escape) }
        runOnIdle {
            assertEquals(0, closes)
            assertEquals(VacancyRecognitionState.Idle, component.recognition.state.value)
        }
        onNodeWithText("Parse PDF").performScrollTo().assertIsEnabled()
    }

    @Test
    fun missingSavedPdfIsVisibleButCannotBeParsedUntilAvailable() = runComposeUiTest {
        val component = TestVacancyEditorComponent()
        val record = VacancyAttachment(
            id = 8,
            createdAt = 0,
            filePath = "attachments/job.pdf",
            fileName = "job.pdf",
            mediaType = "application/pdf",
            fileSize = 10,
            sha256 = "hash",
        )
        component.snapshot.state.value =
            VacancySnapshotState(record = record, status = AttachmentStatus.Missing)
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithText("Parse PDF").performScrollTo().assertIsNotEnabled()
        runOnIdle {
            component.snapshot.state.value =
                VacancySnapshotState(record = record, status = AttachmentStatus.Available)
        }
        onNodeWithText("Parse PDF").assertIsEnabled()
    }

    private fun selected(name: String) =
        VacancySnapshotState(selected = SelectedAttachment("source", name))
}
