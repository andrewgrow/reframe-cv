package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.domain.models.resume.content.ResumeExperience
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumeTextResizeTest : GoldenScreenshotTest() {
    private fun draft() = ResumeDraft("Mobile")
        .addSection(ResumeSummarySection(title = "Professional profile", text = "Summary"))
        .addSection(
            ResumeExperienceSection(
                entries = listOf(
                    ResumeExperience(description = "Context", achievements = listOf("Result")),
                ),
            ),
        )

    @Test
    fun summaryDescriptionAndAchievementsResizeAndKeepTheirEditedValues() =
        runDesktopComposeUiTest {
            val component = TestResumeEditorComponent(draft())
            setGoldenContent(this) { ResumeEditorContent(component) }
            val tags =
                listOf(
                    "resume.summary",
                    "resume.position.description",
                    "resume.position.achievements",
                )
            tags.forEach { tag ->
                val field = onNodeWithTag(tag).performScrollTo()
                val minimum = field.fetchSemanticsNode().size.height
                onNodeWithTag("$tag.resize").performScrollTo().performMouseInput {
                    moveTo(center)
                    press()
                    moveBy(Offset(0f, 20f))
                    moveBy(Offset(0f, 60f))
                    release()
                }
                val expanded = field.fetchSemanticsNode().size.height
                assertTrue(expanded > minimum + 40, tag)
                field.performTextReplacement("Edited $tag\nSecond line")
                assertEquals(expanded, field.fetchSemanticsNode().size.height)
                onNodeWithTag("$tag.resize").performScrollTo().performMouseInput {
                    moveTo(center)
                    press()
                    moveBy(Offset(0f, -20f))
                    moveBy(Offset(0f, -300f))
                    release()
                }
                assertEquals(minimum, field.fetchSemanticsNode().size.height)
            }
            val content = component.draft.value.toResume(1).content
            assertEquals(
                "Edited resume.summary\nSecond line",
                (content.sections[0] as ResumeSummarySection).text,
            )
            val position = (content.sections[1] as ResumeExperienceSection).entries.single()
            assertEquals("Edited resume.position.description\nSecond line", position.description)
            assertEquals(
                listOf("Edited resume.position.achievements", "Second line"),
                position.achievements,
            )
        }

    @Test
    fun resizeHandlesAreDisabledDuringSave() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent(draft())
        setGoldenContent(this) { ResumeEditorContent(component) }
        runOnIdle { component.saveState.value = ResumeSaveState.Saving }
        listOf("resume.summary", "resume.position.description", "resume.position.achievements")
            .forEach { tag -> onNodeWithTag("$tag.resize").assertIsNotEnabled() }
    }

    @Test
    fun expandedSummaryWithCustomTitleMatchesReference() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent(draft())
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.summary.resize").performScrollTo().performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(0f, 20f))
            moveBy(Offset(0f, 100f))
            release()
        }
        onNodeWithTag(
            "resume.summary",
        ).performTextReplacement("Builds reliable Android apps.\nWorks with connected devices.")
        captureGolden(this)
    }
}
