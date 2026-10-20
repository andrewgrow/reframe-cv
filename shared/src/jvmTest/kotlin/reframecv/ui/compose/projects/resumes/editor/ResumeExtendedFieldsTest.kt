package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.resume.content.ResumeEducation
import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.domain.models.resume.content.ResumeExperience
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeLink
import reframecv.domain.models.resume.content.ResumeProject
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumeExtendedFieldsTest : GoldenScreenshotTest() {
    @Test
    fun profileWorkFieldsAndEducationNotesReachSavedContent() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent(
            ResumeDraft("Mobile").addSection(
                ResumeEducationSection(entries = listOf(ResumeEducation())),
            ),
        )
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.profile.workArrangement").performScrollTo()
            .performTextReplacement("Remote or hybrid")
        onNodeWithTag("resume.profile.workAuthorization").performScrollTo()
            .performTextReplacement("RWR+ card holder")
        onNodeWithTag("resume.education.notes").performScrollTo()
            .performTextReplacement("Graduated with honors.\nFaculty award.")
        val content = component.draft.value.toResume(1).content
        assertEquals("Remote or hybrid", content.profile.workArrangement)
        assertEquals("RWR+ card holder", content.profile.workAuthorization)
        val education = content.sections.single() as ResumeEducationSection
        assertEquals("Graduated with honors.\nFaculty award.", education.entries.single().notes)
    }

    @Test
    fun positionDescriptionAndLinksStayScopedToTheirOwners() = runDesktopComposeUiTest {
        val projectLink = ResumeLink("Project", "https://example.com/project")
        val component = TestResumeEditorComponent(
            ResumeDraft("Mobile").addSection(
                ResumeExperienceSection(
                    entries = listOf(
                        ResumeExperience(
                            achievements = listOf("Keep achievement"),
                            projects = listOf(
                                ResumeProject(name = "Project", links = listOf(projectLink)),
                            ),
                        ),
                    ),
                ),
            ),
        )
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.position.description").performScrollTo()
            .performTextReplacement("Production Android app.\nHardware integrations.")
        onAllNodesWithText("Add Link")[0].performScrollTo().performClick()
        onAllNodesWithTag("resume.link.label")[0].performScrollTo()
            .performTextReplacement("Google Play")
        onAllNodesWithTag("resume.link.url")[0].performScrollTo()
            .performTextReplacement("https://example.com/app")
        val entry = (
            component.draft.value.toResume(1).content.sections.single()
                as ResumeExperienceSection
            ).entries.single()
        assertEquals("Production Android app.\nHardware integrations.", entry.description)
        assertEquals(listOf("Keep achievement"), entry.achievements)
        assertEquals(listOf(ResumeLink("Google Play", "https://example.com/app")), entry.links)
        assertEquals(listOf(projectLink), entry.projects.single().links)

        onAllNodesWithTag("resume.link.label")[1].performScrollTo()
            .performTextReplacement("Updated project")
        onAllNodesWithTag("resume.link.url")[1].performScrollTo()
            .performTextReplacement("https://example.com/updated")
        val updatedProjectLink = ResumeLink("Updated project", "https://example.com/updated")

        onAllNodesWithText("Remove Link")[0].performScrollTo().performClick()
        onNodeWithTag("confirmation.input").assertDoesNotExist()
        onNode(hasText("Cancel") and hasAnyAncestor(hasTestTag("resume.item.remove.dialog")))
            .performClick()
        val afterCancel = component.draft.value.sections.single().value as ResumeExperienceSection
        assertEquals(entry.links, afterCancel.entries.single().links)
        onAllNodesWithText("Remove Link")[0].performScrollTo().performClick()
        onNode(hasText("Remove Link") and hasAnyAncestor(hasTestTag("resume.item.remove.dialog")))
            .performClick()
        val updated = (component.draft.value.sections.single().value as ResumeExperienceSection)
            .entries.single()
        assertEquals(emptyList(), updated.links)
        assertEquals(listOf(updatedProjectLink), updated.projects.single().links)
    }
}
