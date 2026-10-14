package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeImportState
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumeValidationFeedbackTest : GoldenScreenshotTest() {
    @Test
    fun failedSaveNotificationStaysVisibleWhenScrolledAndIsIndependentOfCorrection() {
        val record = Resume(id = 1, projectId = 1, name = "Mobile", createdAt = 0, updatedAt = 0)
        listOf(null, record).forEach { initial ->
            runDesktopComposeUiTest {
                var saves = 0
                val component = TestResumeEditorComponent(
                    initialDraft = ResumeDraft("Mobile").addSection(ResumeSummarySection()),
                    initialResume = initial,
                    save = { saves++ },
                )
                setGoldenContent(this) { ResumeEditorContent(component) }
                onNodeWithTag("resume.name").performTextReplacement(" ")
                onNodeWithTag("resume.consent").performScrollTo()
                onNodeWithTag("resume.name").assertIsNotDisplayed()
                val notification = onNode(
                    hasText("Validation failed. Please check the form.") and
                        hasAnyAncestor(hasTestTag("resume.feedback")),
                )
                notification.assertDoesNotExist()
                val bounds = onNodeWithTag("resume.feedback").fetchSemanticsNode().boundsInRoot
                runOnIdle {
                    if (initial == null) {
                        component.importComponent.state.value = ResumeImportState.Failed
                    } else {
                        component.saveState.value = ResumeSaveState.Failed
                    }
                }
                onNodeWithText("Save").performClick()
                onNodeWithTag("resume.consent").performScrollTo()
                notification.assertIsDisplayed()
                assertEquals(0, saves)
                assertEquals(
                    bounds,
                    onNodeWithTag("resume.feedback").fetchSemanticsNode().boundsInRoot,
                )
                onNodeWithTag("resume.name").performScrollTo().performTextReplacement("Mobile")
                notification.assertIsDisplayed()
                onNodeWithText("Save").performClick()
                assertEquals(1, saves)
            }
        }
    }

    @Test
    fun notificationExpiresDespiteInvalidDraftAndRepeatedSaveRestartsThreeSecondTimer() =
        runDesktopComposeUiTest {
            var saves = 0
            val component = TestResumeEditorComponent(save = { saves++ })
            mainClock.autoAdvance = false
            setGoldenContent(this) { ResumeEditorContent(component) }
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            val notification = onNodeWithText("Validation failed. Please check the form.")
            notification.assertDoesNotExist()
            onNodeWithText("Save").performClick()
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            notification.assertIsDisplayed()
            mainClock.advanceTimeBy(2_000)
            notification.assertIsDisplayed()
            onNodeWithText("Save").performClick()
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeByFrame()
            mainClock.advanceTimeBy(2_000)
            notification.assertIsDisplayed()
            mainClock.advanceTimeBy(1_100)
            notification.assertDoesNotExist()
            assertEquals(false, component.draft.value.isValid)
            assertEquals(2L, component.validationFailures.value)
            assertEquals(0, saves)
            runOnIdle { component.onDraftChange(ResumeDraft("Valid name")) }
            mainClock.advanceTimeByFrame()
            runOnIdle { component.onDraftChange(ResumeDraft()) }
            mainClock.advanceTimeByFrame()
            notification.assertDoesNotExist()
        }
}
