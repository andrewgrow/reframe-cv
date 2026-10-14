package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.resume.content.ResumeDate
import reframecv.domain.models.resume.content.ResumeDateRange
import reframecv.domain.models.resume.content.ResumeEducation
import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.domain.models.resume.content.ResumeExperience
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeProject
import reframecv.domain.models.resume.content.ResumeSkillOrTool
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumeExperienceRemovalTest : GoldenScreenshotTest() {
    @Test
    fun educationRemovalRequiresConfirmationAndKeepsDatesMatched() = runDesktopComposeUiTest {
        val first = ResumeEducation(
            institution = "Discard university",
            period = ResumeDateRange(start = ResumeDate(2010)),
        )
        val second = ResumeEducation(
            institution = "Keep university",
            period = ResumeDateRange(start = ResumeDate(2020)),
        )
        val draft = ResumeDraft("Mobile")
            .addSection(ResumeEducationSection(entries = listOf(first, second)))
        val component = TestResumeEditorComponent(draft)
        setGoldenContent(this) { ResumeEditorContent(component) }
        onAllNodesWithText("Remove Education Entry")[0].performScrollTo().performClick()
        assertEquals(draft, component.draft.value)
        onNodeWithTag("confirmation.input").assertDoesNotExist()
        onNode(
            hasText("Cancel") and hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
        ).performClick()
        assertEquals(draft, component.draft.value)
        onAllNodesWithText("Remove Education Entry")[0].performScrollTo().performClick()
        onNodeWithTag("resume.item.remove.dialog").performKeyInput { pressKey(Key.Escape) }
        assertEquals(draft, component.draft.value)
        onAllNodesWithText("Remove Education Entry")[0].performScrollTo().performClick()
        onNode(
            hasText("Remove Education Entry") and
                hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
        ).performClick()
        val remaining = component.draft.value.sections.single()
        assertEquals(listOf(second), (remaining.value as ResumeEducationSection).entries)
        assertEquals(listOf(draft.sections.single().periods.last()), remaining.periods)
    }

    @Test
    fun removingProjectsAndPositionsRequiresConfirmationAndKeepsPeriodsMatched() =
        runDesktopComposeUiTest {
            val keepProject = ResumeProject(name = "Keep project")
            val discardProject = ResumeProject(name = "Discard project")
            val first = ResumeExperience(
                position = "First position",
                period = ResumeDateRange(start = ResumeDate(2010)),
                projects = listOf(discardProject, keepProject),
            )
            val second = ResumeExperience(
                position = "Keep position",
                period = ResumeDateRange(start = ResumeDate(2020)),
            )
            val original = ResumeDraft("Mobile").addSection(
                ResumeExperienceSection(entries = listOf(first, second)),
            )
            var saves = 0
            var closes = 0
            val component =
                TestResumeEditorComponent(original, save = { saves++ }, close = { closes++ })
            setGoldenContent(this) { ResumeEditorContent(component) }
            onAllNodesWithText("Remove Project")[0].performScrollTo().performClick()
            onNodeWithTag("confirmation.input").assertDoesNotExist()
            assertEquals(original, component.draft.value)
            onNode(
                hasText("Cancel") and hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
            ).performClick()
            assertEquals(original, component.draft.value)
            onAllNodesWithText("Remove Project")[0].performScrollTo().performClick()
            onNodeWithTag("resume.item.remove.dialog").performKeyInput { pressKey(Key.Escape) }
            assertEquals(original, component.draft.value)
            onAllNodesWithText("Remove Project")[0].performScrollTo().performClick()
            onNode(
                hasText("Remove Project") and
                    hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
            ).performClick()
            val afterProject = component.draft.value.sections.single()
            val section = afterProject.value as ResumeExperienceSection
            assertEquals(listOf(keepProject), section.entries.first().projects)
            assertEquals(second, section.entries.last())
            assertEquals(original.sections.single().periods, afterProject.periods)
            onAllNodesWithText("Remove Position")[0].performScrollTo().performClick()
            val beforePosition =
                component.draft.value.sections.single().value as ResumeExperienceSection
            assertEquals(2, beforePosition.entries.size)
            onNode(
                hasText("Remove Position") and
                    hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
            ).performClick()
            val remaining = component.draft.value.sections.single()
            assertEquals(listOf(second), (remaining.value as ResumeExperienceSection).entries)
            assertEquals(listOf(original.sections.single().periods.last()), remaining.periods)
            assertEquals(0, saves)
            assertEquals(0, closes)
        }

    @Test
    fun removingToolRequiresConfirmationAndKeepsItsProject() = runDesktopComposeUiTest {
        val keep = ResumeSkillOrTool(name = "Keep tool")
        val project =
            ResumeProject(
                name = "Project",
                skillsOrTools = listOf(ResumeSkillOrTool("Discard"), keep),
            )
        val original = ResumeDraft("Mobile").addSection(
            ResumeExperienceSection(entries = listOf(ResumeExperience(projects = listOf(project)))),
        )
        val component = TestResumeEditorComponent(original)
        setGoldenContent(this) { ResumeEditorContent(component) }
        onAllNodesWithText("Remove Skill or Tool")[0].performScrollTo().performClick()
        assertEquals(original, component.draft.value)
        onNode(
            hasText("Remove Skill or Tool") and
                hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
        ).performClick()
        val section = component.draft.value.sections.single().value as ResumeExperienceSection
        assertEquals(
            listOf(project.copy(skillsOrTools = listOf(keep))),
            section.entries.single().projects,
        )
    }
}
