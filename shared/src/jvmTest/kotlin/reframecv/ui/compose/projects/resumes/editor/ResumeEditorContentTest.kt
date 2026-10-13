package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumeEditorContentTest : GoldenScreenshotTest() {
    @Test
    fun validatesNameAndSavesProfileSummaryAndKeywordsWithFixedControls() =
        runDesktopComposeUiTest {
            var writes = 0
            val component = TestResumeEditorComponent(save = { writes++ })
            setGoldenContent(this) { ResumeEditorContent(component) }
            onNodeWithText("Save").performClick()
            onNodeWithText("Enter a resume name.").assertExists()
            assertEquals(0, writes)
            onNodeWithTag("resume.name").performTextReplacement("Mobile")
            onNodeWithText("Full name").performScrollTo().performTextReplacement("Alex")
            onNodeWithText(
                "Keywords (comma-separated)",
            ).performScrollTo().performTextReplacement("Kotlin, Compose")
            onNodeWithText("Add section").performScrollTo().performClick()
            onNodeWithText("Summary").performClick()
            onNodeWithText(
                "Text",
            ).performScrollTo().performTextReplacement("Builds reliable applications.")
            onNodeWithText("Save").performClick()
            assertEquals(1, writes)
            val resume = component.draft.value.toResume(7)
            assertEquals("Alex", resume.content.profile.name)
            assertEquals(listOf("Kotlin", "Compose"), resume.keywords)
            assertEquals(
                "Builds reliable applications.",
                assertIs<ResumeSummarySection>(resume.content.sections.single()).text,
            )
        }

    @Test
    fun educationDatesRejectPartialInvalidInputAndPresentOmitsEndDate() = runDesktopComposeUiTest {
        var writes = 0
        val component = TestResumeEditorComponent(save = { writes++ })
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.name").performTextReplacement("Draft")
        onNodeWithText("Add section").performScrollTo().performClick()
        onNodeWithText("Education").performClick()
        onNodeWithText("Add entry").performScrollTo().performClick()
        onNodeWithText("Institution").performScrollTo().performTextReplacement("University")
        onNodeWithText("Start (YYYY or YYYY-MM)").performScrollTo().performTextReplacement("2020-")
        onNodeWithText("Save").performClick()
        assertEquals(0, writes)
        onNodeWithText("Use YYYY or YYYY-MM, with a valid year and month.").assertExists()
        onNodeWithText("Start (YYYY or YYYY-MM)").performScrollTo().performTextReplacement("2020")
        onNodeWithText("End (YYYY or YYYY-MM)").performScrollTo().performTextReplacement("invalid")
        onNodeWithText("Present").performScrollTo().performClick()
        onNodeWithText("Save").performClick()
        assertEquals(1, writes)
        val entry = assertIs<ResumeEducationSection>(
            component.draft.value.toResume(1).content.sections.single(),
        ).entries.single()
        assertEquals(2020, entry.period.start?.year)
        assertEquals(null, entry.period.end)
        assertEquals(true, entry.period.isCurrent)
    }

    @Test
    fun experienceSupportsProfessionalProjectsAndCategorizedTools() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent()
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.name").performTextReplacement("Developer")
        onNodeWithText("Add section").performScrollTo().performClick()
        onNodeWithText("Work experience").performClick()
        onNodeWithText("Add entry").performScrollTo().performClick()
        onNodeWithText("Position").performScrollTo().performTextReplacement("Mobile developer")
        onNodeWithText("Add project").performScrollTo().performClick()
        onNodeWithText("Project name").performScrollTo().performTextReplacement("Banking app")
        onNodeWithText("Add skill or tool").performScrollTo().performClick()
        onNodeWithText("Skill or tool").performScrollTo().performTextReplacement("Kotlin")
        onNodeWithText("Category").performScrollTo().performTextReplacement("Language")
        val entry = assertIs<ResumeExperienceSection>(
            component.draft.value.toResume(1).content.sections.single(),
        ).entries.single()
        assertEquals("Mobile developer", entry.position)
        assertEquals("Banking app", entry.projects.single().name)
        assertEquals("Language", entry.projects.single().skillsOrTools.single().category)
    }

    @Test
    fun cancelEscapeAndBusyStateBehaveLikeVacancyEditor() = runDesktopComposeUiTest {
        var closes = 0
        val component = TestResumeEditorComponent(close = { closes++ })
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.name").performKeyInput { pressKey(Key.Escape) }
        assertEquals(1, closes)
        onNodeWithText("Cancel").performClick()
        assertEquals(2, closes)
        runOnIdle { component.saveState.value = ResumeSaveState.Saving }
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithTag("resume.name").performKeyInput { pressKey(Key.Escape) }
        assertEquals(2, closes)
        assertFalse(component.draft.value.isValid)
    }
}
